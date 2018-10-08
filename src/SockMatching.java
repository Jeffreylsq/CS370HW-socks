import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

public class SockMatching {
    
   
    private static boolean Production = false;
    private static boolean Matching = false;
    private static final int maxSocks = 100;
    private static final BlockingQueue<Sock> Match = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Sock> Wash = new LinkedBlockingQueue<>();
    private static final AtomicInteger[] colorsP = new AtomicInteger[4];
    private static final AtomicInteger[] colorsM = new AtomicInteger[4];
    private static final AtomicInteger[] colorsW= new AtomicInteger[4];
    
    public static void main(String[] args) {
        try { 
            for(int i = 0; i < 4; i++) {
                colorsP[i] = new AtomicInteger();
                colorsM[i] = new AtomicInteger();
                colorsW[i] = new AtomicInteger();
            }
            
            int numProducers = 4;

            Thread[] producers = new Thread[numProducers];
            
            for(int i = 0; i < numProducers; i++)
                producers[i] = new Thread(new SockProducer());

            Thread matcher = new Thread(new SockMatcher());
            Thread washer = new Thread(new SockWasher());
            
            for(int i = 0; i < numProducers; i++)
                producers[i].start();
            
            matcher.start();
            washer.start();

            for(int i = 0; i < numProducers; i++)
                producers[i].join();
            
            Production = true;
            
            matcher.join();
            
            Matching = true;
            
            washer.join();
            
            System.out.println("---------------------------------------");
            for(int i = 0; i < 4; i++) {
                String colorString = colorToString(i);
                System.out.println("Total " + colorString
                        + " Sock production: " + colorsP[i]);
                System.out.println("Total " + colorString
                        + " Pairs total: " + colorsM[i]);
                System.out.println("Total " + colorString
                        + " Pairs washed: " + colorsW[i]);
                
                Color thisColor = intToColor(i);
                int unmatched = 0;
                for(int j = 0; j < Match.size(); j++) {
                    try {
                        Sock sock = Match.take();
                        Match.put(sock);
                        if(sock.color() == thisColor) {
                            unmatched++;
                            break;
                        }
                    } catch(InterruptedException e) { }
                }
                System.out.println("Total " + colorString
                        + " Socks remaining: " + unmatched);
                System.out.println("---------------------------------------");
            }
            
        } catch(Exception e) { }
        
    }
    

   
    
    
    
    private static class SockMatcher implements Runnable {
        
        private boolean match() {
            boolean matchFound = false;
            if(!Match.isEmpty()) {
                try {
                    Sock first = Match.take();
                    int mismatches = 0;
                    matchFound = false;

                    while(mismatches < Match.size()) {
                        Sock second = Match.take();

                        if(second.color() == first.color()) {
                            matchFound = true;
                            Wash.put(first);
                            Wash.put(second);
                            colorsM[colorToInt(first.color())]
                                    .incrementAndGet();
                            break;
                        } else {
                        	Match.put(second);
                            mismatches++;
                        }
                    }

                    if(matchFound)
                        System.out.println("Matcher: Sent "
                                + colorToString(first.color())
                                + " pair to washer.");
                    else
                    	Match.put(first);
                } catch(InterruptedException e) { }
            }
            return matchFound;
        }
        
        @Override
        public void run() {
            while(!Production || Match.size() > 4)
                match();
            for(int i = 0; i < 2; i++)
                match();
        }
        
    }
    
    private static class SockWasher implements Runnable {
        
        private boolean wash() {
            boolean matchFound = false;
            if(!Wash.isEmpty()) {
                try {
                    Sock first = Wash.take();
                    int mismatches = 0;
                    matchFound = false;

                    while(mismatches < Wash.size()) {
                        Sock second = Wash.take();

                        if(second.color() == first.color()) {
                            matchFound = true;
                            colorsW[colorToInt(first.color())]
                                    .incrementAndGet();
                            break;
                        } else {
                        	Wash.put(second);
                            mismatches++;
                        }
                    }

                    if(matchFound)
                        System.out.println(" Washed "
                                + colorToString(first.color())
                                + " pair.");
                    else
                    	Wash.put(first);
                } catch(InterruptedException e) { }
            }
            return matchFound;
        }
        
        @Override
        public void run() {
            while(!Production || !Matching
                    || Wash.size() > 0) {
                wash();
            }
        }
        
    }
    
    private static void printQueue(BlockingQueue<Sock> q) {
        for(Sock s : q)
            System.out.print(colorToString(s.color()) + " ");
        System.out.println();
    }
    
    private static String colorToString(Color color) {
        switch(color) {
            case Red:
                return "RED";
            case Orange:
                return "ORANGE";
            case Green:
                return "GREEN";
            case Blue:
                return "BLUE";
            default:
                return "";
        }
    }
    
    private static String colorToString(int i) {
        switch(i) {
            case 0:
                return colorToString(Color.Red);
            case 1:
                return colorToString(Color.Orange);
            case 2:
                return colorToString(Color.Green);
            case 3:
                return colorToString(Color.Blue);
            default:
                return "";
        }
    }
    
    private static int colorToInt(Color color) {
        switch(color) {
            case Red:
                return 0;
            case Orange:
                return 1;
            case Green:
                return 2;
            case Blue:
                return 3;
            default:
                return -1;
        }
    }
    
    private static Color intToColor(int i) {
        switch(i) {
            case 0:
                return Color.Red;
            case 1:
                return Color.Orange;
            case 2:
                return Color.Green;
            default:
                return Color.Blue;
        }
    }
    
private static class SockProducer implements Runnable {
        
        private final int EXPECTED_YIELD
                = ThreadLocalRandom.current().nextInt(1, maxSocks + 1);
        private int currentYield = 0;
        private int[] colorYield = new int[4];
        
        @Override
        public void run() {
            while(currentYield < EXPECTED_YIELD) {
                int colorCode = ThreadLocalRandom.current().nextInt(0, 4);
                try {
                    switch(colorCode) {
                        case 0:
                            Match.put(new Sock(Color.Red));
                            break;
                        case 1:
                        	Match.put(new Sock(Color.Orange));
                            break;
                        case 2:
                        	Match.put(new Sock(Color.Green));
                            break;
                        case 3:
                        	Match.put(new Sock(Color.Blue));
                            break;
                    }
                    currentYield++;
                    colorYield[colorCode]++;
                    colorsP[colorCode].incrementAndGet();
                } catch(InterruptedException e) { }
            }
            
            long producerID = Thread.currentThread().getId();
            for(int i = 0; i < 4; i++) {
                System.out.println("Product Thread" + producerID + ": Made "
                        + colorYield[i] + " " + colorToString(i) + " socks.");
            }
        }
        
    }
}