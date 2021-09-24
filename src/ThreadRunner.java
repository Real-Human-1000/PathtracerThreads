public class ThreadRunner extends Thread {

    private int HI = 1; // Halton Index, shortened to make code more readable
    private final double haltbase1 = 3;
    private final double haltbase2 = 2;
    private int width;
    private int height;
    private final double PI = 3.1415926535;
    private final double refrIndex = 1.5;
    private final int spp = 2048;
    private Vector[] pixelRow;
    private int y;

    private Sphere[] objects;


    public ThreadRunner (Sphere[] o, int w, int h, int iy, Vector[] p) {
        objects = o;
        width = w;
        height = h;
        pixelRow = p;
        y = iy;
    }

    public void run() {
        sampleRow();
    }

    public double halton(double b) {
        double f = 1;
        double r = 0;
        double i = HI;

        while(i > 0) {
            f = f / b;
            r = r + f * (i % b);
            i = (int) (i / b);
        }

        HI = HI + 1;
        return r;
    }

    public Vector ofProjection(double x, double y) { // Maybe change name; 3D coordinates of 2D pixels
        double w = width;
        double h = height;
        double fovx = PI/4;
        double fovy = (h/w)*fovx;
        return new Vector(((2 * x - w) / w) * Math.tan(fovx),
                -((2 * y - h) / h) * Math.tan(fovy),
                -1);
    }

    public Vector hemisphere(double u1, double u2) {
        double r = Math.sqrt(1 - (u1*u1));
        double phi = 2 * PI * u2;
        return new Vector(Math.cos(phi) * r, Math.sin(phi) * r, u1);
    }

    public void trace(Ray ray, Sphere[] objects, int depth, Vector clr) {
        if (depth >= 4) { return; }

        double t; // time steps; like distance
        int id = -1;
        double mint = Double.MAX_VALUE; // minimum t

        for (int o = 0; o < objects.length; o++) {
            t = objects[o].intersection(ray);
            if (t > 0 && t < mint) { mint = t; id = o; }
        }

        if (id == -1) { return; }

        Vector hp = Vector.add(ray.getOrigin(), Vector.mult(ray.getDirection(),mint)); // "Hit Point"
        Vector N = objects[id].surfaceNormal(hp).norm();
        ray.setOrigin(hp);
        clr.add(Vector.mult(objects[id].getMaterial().getColor(), objects[id].getMaterial().getEmission()));

        if (objects[id].getMaterial().getType() == 1) { // Diffuse BRDF
            ray.setDirection(Vector.add(N,hemisphere(halton(haltbase1),halton(haltbase2))));
            double cost = Vector.dot(ray.getDirection(), N);
            Vector tmp = new Vector();
            trace(ray, objects, depth+1, tmp);

            clr.add(tmp.mult(objects[id].getMaterial().getColor()).mult(cost*0.1));
        }

        if (objects[id].getMaterial().getType() == 2) { // Specular BRDF
            double cost = ray.getDirection().dot(N);

            N.mult(cost*2);
            ray.getDirection().sub(N);

            ray.getDirection().add(Vector.random3D().mult(objects[id].getRadius()*objects[id].getMaterial().getRoughness()));

            ray.getDirection().norm();

            Vector tmp = new Vector();
            trace(ray, objects, depth+1, tmp);
            clr.add(tmp);
        }

        if (objects[id].getMaterial().getType() == 3) { // Refractive BRDF
            double n = refrIndex;
            double R0 = (1.0-n) / (1.0+n);
            R0 = R0 * R0;
            if (N.dot(ray.getDirection()) > 0) {
                N.mult(-1);
                n = 1/n;
            }
            n = 1/n;
            double cosin = N.dot(ray.getDirection())*-1;
            double Rprob = R0 + (1.0-R0) * Math.pow(1.0-cosin, 5);
            double cost2 = 1.0-(n*n)*(1.0-(cosin*cosin));

            if (cost2 > 0 && Math.random() > Rprob) { // refraction
                ray.getDirection().mult(n);
                N.mult(n*cosin-Math.sqrt(cost2));
                ray.getDirection().add(N);
                ray.getDirection().add(Vector.random3D().mult(objects[id].getRadius()*objects[id].getMaterial().getRoughness()));

                ray.getDirection().norm();
            }
            else { // reflection
                N.mult(cosin*2);
                ray.getDirection().add(N);
                ray.getDirection().add(Vector.random3D().mult(objects[id].getRadius()*objects[id].getMaterial().getRoughness()));
                ray.getDirection().norm();
            }

            Vector tmp = new Vector();
            trace(ray, objects, depth+1, tmp);
            clr.add(tmp.mult(1.15));
        }
    }

    public void sampleRow() {
        for (int x = 0; x < width; x++) {
            for (int s = 0; s < spp; s++) {
                Vector c = new Vector();
                Ray ray = new Ray(new Vector(), new Vector());
                Vector cam = ofProjection(x,y);
                cam.setX(cam.getX() + Math.random()/700 - 1.0/1400);
                cam.setY(cam.getY() + Math.random()/700 - 1.0/1400);

                cam.sub(ray.getOrigin());
                cam.norm();
                ray.setDirection(cam);

                trace(ray, objects, 0, c);
                pixelRow[x].add(c.div(spp));
            }
        }
    }


}
