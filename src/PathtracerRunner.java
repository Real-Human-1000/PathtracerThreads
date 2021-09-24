import java.lang.Math;

public class PathtracerRunner {
    private final int width = 500;
    private final int height = 500;

    int[][] colors = new int[9][4];
    // private final String asciiGrays = "█.`:,;'_^\"></-!~=)(|j?}{][ti+l7v1%yrfcJ32uIC$zwo96sngaT5qpkYVOL40&*mG8xhedbZUSAQPFDXWK#RNEHBM@";
    private final String asciiGrays = "█.:-=+*#%@";  // ASCII representation of gray characters
    Vector[][] pixels = new Vector[height][width];
    Sphere[] objects;
    ThreadRunner[] threads;

    public static void main(String[] args) {
        PathtracerRunner runner = new PathtracerRunner();
        runner.setup();
        runner.draw();

    }

    public double globalHDRMap(double x, double t, double min, double max) {
        return 255 * ( Math.log(x + t) - Math.log(min + t) ) / ( Math.log(max + t) - Math.log(min + t) );
    }

    public void drawPixels() {
        // Find global brightest and darkest
        Vector brightest = new Vector();
        Vector darkest = new Vector(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        Vector average = new Vector();
        double size = height * width;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                brightest.setX(Math.max(pixels[y][x].getX(), brightest.getX()));
                darkest.setX(Math.min(pixels[y][x].getX(), darkest.getX()));
                if (darkest.getX() < 0) {
                    darkest.setX(0);
                }

                brightest.setY(Math.max(pixels[y][x].getY(), brightest.getY()));
                darkest.setY(Math.min(pixels[y][x].getY(), darkest.getY()));
                if (darkest.getY() < 0) {
                    darkest.setY(0);
                }

                brightest.setZ(Math.max(pixels[y][x].getZ(), brightest.getZ()));
                darkest.setZ(Math.min(pixels[y][x].getZ(), darkest.getZ()));
                if (darkest.getZ() < 0) {
                    darkest.setZ(0);
                }

                average.add(Vector.div(pixels[y][x], size));
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get average of <x, y, z> (AKA <red, green, blue>), or the grayscale value
                double hdrRed = globalHDRMap(pixels[y][x].getX(), 1, darkest.getX(), brightest.getX());
                double hdrGreen = globalHDRMap(pixels[y][x].getY(), 1, darkest.getY(), brightest.getY());
                double hdrBlue = globalHDRMap(pixels[y][x].getZ(), 1, darkest.getZ(), brightest.getZ());

                // System.out.println("(" + hdrRed + ", " + hdrGreen + ", " + hdrBlue + ") (" + pixels[y][x].getX() + ", " + pixels[y][x].getY() + ", " + pixels[y][x].getZ() + ")");

                double grayscale = (hdrRed + hdrGreen + hdrBlue) / 3;
                // double grayscale = (pixels[y][x].getX() + pixels[y][x].getY() + pixels[y][x].getZ()) / 3;
                int asciiIndex = (int) (grayscale / (255 / asciiGrays.length()));
                if (asciiIndex > asciiGrays.length() - 1) { asciiIndex = asciiGrays.length() - 1; }

                double leastdist = Double.MAX_VALUE;
                int colorIndex = 0;
                for (int c = 0; c < colors.length; c++) {
                    double distdist = Math.pow(pixels[y][x].getX() - colors[c][0], 2) + Math.pow(pixels[y][x].getY() - colors[c][1], 2) + Math.pow(pixels[y][x].getZ() - colors[c][2], 2);
                    if (distdist < leastdist) { leastdist = distdist; colorIndex = c; }
                }

                System.out.print("\033[;3" + colorIndex + "m" + asciiGrays.charAt(asciiIndex) + asciiGrays.charAt(asciiIndex) + "\033[0m");
            }
            System.out.println();
        }
    }



    public void setColors() {
        colors[0] = new int[]{0,0,0};
        colors[1] = new int[]{240,82,79};
        colors[2] = new int[]{92,150,44};
        colors[3] = new int[]{166,138,13};
        colors[4] = new int[]{57,147,212};
        colors[5] = new int[]{167,113,191};
        colors[6] = new int[]{0,163,163};
        colors[7] = new int[]{128,128,128};
        colors[8] = new int[]{187,187,187};
    }

    public void setup() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = new Vector();
            }
        }
        setColors();

        // For now, only Spheres can be used
        objects = new Sphere[4];

        Material greenMaterial = new Material(new Vector(32,128,32), 0, 0, 1);
        Material blueMaterial = new Material(new Vector(32, 32, 128), 0, 0, 1);
        Material mirrorMaterial = new Material(new Vector(128,128,128), 0, 0, 2);
        Material metalMaterial = new Material(new Vector(200,200,200), 0, 0.5, 2);
        Material glassMaterial = new Material(new Vector(128,128,128), 0, 0.2, 3);
        Material lightMaterial = new Material(new Vector(128,128,128), 0.2, 0, 1);

        Sphere bigSphere = new Sphere(new Vector(0,0,-9), 2.75, greenMaterial);
        objects[0] = bigSphere;

        Sphere smallSphere1 = new Sphere(new Vector(2,0,-7), 0.5, blueMaterial);
        objects[1] = smallSphere1;

        Sphere smallSphere2 = new Sphere(new Vector(-2,0,-7), 0.5, blueMaterial);
        objects[2] = smallSphere2;

        Sphere lightSphere = new Sphere(new Vector(0,6,-6), 1, lightMaterial);
        objects[3] = lightSphere;
    }



    public void draw() {
        threads = new ThreadRunner[height];
        for (int y = 0; y < height; y++) {
            threads[y] = new ThreadRunner(objects, width, height, y, pixels[y]);
            threads[y].start();
        }

        int numLoops = 0;
        boolean threadsRunning = true;
        while (threadsRunning) {
            numLoops += 1;
            threadsRunning = false;
            for (int y = 0; y < height; y++) {
                if (threads[y].isAlive()) {
                    threadsRunning = true;
                }
            }
        }

        drawPixels();
        System.out.println(numLoops);
    }
}
