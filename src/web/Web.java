package web;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Web {
    private static final int MIN_SIDES = 3;
    private static final int MAX_SIDES = 30;

    public static int width = 600;
    public static int height = 600;

    private static int webSidesCount = 15;

    private static Point center = new Point(width / 2, height / 2);
    private static final int minDistanceBetweenSkeletonPoints = Math.min(height, width) / 5;
    private static final int minSkeletonDistanceFromCenter = 2 * minDistanceBetweenSkeletonPoints;
    private static final int minTrappingNetCircleDistance = Math.min(height, width) / 75;
    private static final int flySize = Math.min(width, height) / 50;
    private static final int fliesCount = 1000;
    private static final double minAngleBetweenSkeletonLines = 2 * Math.PI / (3 * webSidesCount);
    private static final double trappingNetCirclesDispersion = 7.0;
    private static Random random = new Random();

    private int generation = 0;
    private double efficiency = 0;

    private WebSkeleton skeleton;
    private ArrayList<TrappingNetCircle> trappingNet;
    private ArrayList<Fly> flies;
    private ArrayList<Fly> caughtFlies;

    public static boolean drawFlies = false;

    public static void setSidesCount(int count) throws IllegalArgumentException {
        if (count < MIN_SIDES || count > MAX_SIDES)
            throw new IllegalArgumentException("Web sides count should be in range ["
                    + MIN_SIDES + ", " + MAX_SIDES + "]");
        webSidesCount = count;
    }

    public static int getSidesCount() {
        return webSidesCount;
    }

    public Web() {
        flies = new ArrayList<Fly>(fliesCount);
        for (int i = 0; i < fliesCount; i++) {
            flies.add(new Fly());
        }
        build();
    }

    public double getEfficiency() {
        return efficiency;
    }

    void build(){
        skeleton = new WebSkeleton();
        trappingNet = new ArrayList<TrappingNetCircle>();
        skeleton.generate();
        generateTrappingNet();

        calculateEfficiency();
    }

    private void calculateEfficiency() {
        caughtFlies = new ArrayList<Fly>();
        int caught = 0;
        for (Fly fly: flies) {
            if (fly.gotCaught()) {
                caught++;
                caughtFlies.add(fly);
            }
        }
        efficiency = (double) caught / fliesCount;
    }


    private void generateTrappingNet() {
        trappingNet.clear();

        while (true) {
            TrappingNetCircle circle = new TrappingNetCircle();
            if (circle.fitsToWeb())
                trappingNet.add(circle);
            else break;
        }
    }

    void draw(Graphics2D g) {
        skeleton.draw(g);
        drawTrappingNet(g);
        if (drawFlies)
            drawCaughtFlies(g);
    }

    private void drawCaughtFlies(Graphics2D g) {
        for (Fly f : caughtFlies)
            f.draw(g);
    }

    private void drawCenterPoint(Graphics2D g) {
        g.fillOval(center.x - 2, center.y - 2, 4, 4);
    }

    private Polygon getPolygonFromPolarPointsList(ArrayList<PolarPoint> list) {
        int[] xPoints = new int[list.size()];
        int[] yPoints = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            Point p = list.get(i).getCartesianPoint();
            xPoints[i] = p.x;
            yPoints[i] = p.y;
        }

        return new Polygon(xPoints, yPoints, list.size());
    }

    private void drawTrappingNet(Graphics2D g) {
        Color oldColor = g.getColor();
        Stroke oldStroke = g.getStroke();
        g.setColor(new Color(255, 0, 0));
        g.setStroke(new BasicStroke(2));
        for (TrappingNetCircle circle : trappingNet)
            g.drawPolygon(circle.getPolygon());
        g.setColor(oldColor);
        g.setStroke(oldStroke);
    }

    private Polygon getPolygonFromPointsArray(ArrayList<Point> list) {
        int[] xPoints = new int[list.size()];
        int[] yPoints = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            xPoints[i] = list.get(i).x;
            yPoints[i] = list.get(i).y;
        }

        return new Polygon(xPoints, yPoints, xPoints.length);
    }

    private class TrappingNetCircle {
        private ArrayList<PolarPoint> list;
        private Polygon polygon;

        public boolean fitsToWeb() {
            return fits;
        }

        private boolean fits = false;

        public TrappingNetCircle() {
            list = new ArrayList<PolarPoint>();
            generateTrappingNetCircle();
        }

        public Polygon getPolygon() {
            return polygon;
        }

        private void generatePolygon() {
            int n = list.size();
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            for (int i = 0; i < n; i++) {
                Point p = list.get(i).getCartesianPoint();
                xPoints[i] = p.x;
                yPoints[i] = p.y;
            }
            polygon = new Polygon(xPoints, yPoints, n);
            polygon.translate(center.x, center.y);
        }

        private void generateTrappingNetCircle() {
            for (int i = 0; i < webSidesCount; i++) {
                int lowerBound = minTrappingNetCircleDistance;
                if (!trappingNet.isEmpty()) {
                    lowerBound += trappingNet.get(trappingNet.size() - 1).list.get(i).distance;
                }
                int maxDistance = skeleton.points.get(i).distance - minTrappingNetCircleDistance;

                if (lowerBound > maxDistance) {
                    fits = false;
                    return;
                }

                int upperBound = Math.min(maxDistance, lowerBound +
                        (int) (trappingNetCirclesDispersion * minTrappingNetCircleDistance));
                double angle = skeleton.points.get(i).angle;

                int distance = (int) (lowerBound + random.nextDouble() * (upperBound - lowerBound));
                list.add(new PolarPoint(angle, distance));
            }
            generatePolygon();
            fits = true;
        }
    }

    private class Fly {
        private Rectangle2D rect;

        public Fly() {
            int x = random.nextInt() % (width / 2 - flySize);
            int y = random.nextInt() % (height / 2 - flySize);
            rect = new Rectangle2D.Float(x, y, flySize, flySize);
        }

        public boolean gotCaught() {
            for (TrappingNetCircle innerCircle : trappingNet) {
                for (int i = 0; i < innerCircle.list.size(); i++) {
                    Point a = innerCircle.list.get(i).getCartesianPoint();
                    Point b = innerCircle.list.get((i + 1) % innerCircle.list.size()).getCartesianPoint();
                    Line2D line2D = new Line2D.Float(a, b);
                    if (intersectsWithLine(line2D))
                        return true;
                }
            }
            return false;
        }

        private boolean intersectsWithLine(Line2D line) {
            return rect.intersectsLine(line);
        }


        public void draw(Graphics2D g) {
            g.setColor(new Color(94, 104, 205, 128));
            g.setBackground(new Color(0, 0, 0));
            g.fillRect((int) rect.getX() + center.x, (int) rect.getY() + center.y, flySize, flySize);
            g.drawRect((int) rect.getX() + center.x, (int) rect.getY() + center.y, flySize, flySize);
        }
    }

    private class WebSkeleton {
        private Polygon polygon;
        private ArrayList<PolarPoint> points;

        public WebSkeleton() {
            points = new ArrayList<PolarPoint>();
        }

        private void generate() {
            do {
                generateSkeletonPoints();

                Collections.sort(points);

                generateSkeletonPolygon();
                polygon.translate(center.x, center.y);
            }
            while(!skeletonIsValid());
        }

        private void generateSkeletonPoints() {
            points.clear();

            for (int i = 0; i < webSidesCount; i++) {
                PolarPoint p = generateSkeletonPoint();
                while (!skeletonPointIsValid(p, points))
                    p = generateSkeletonPoint();
                points.add(p);
            }
        }

        private PolarPoint generateSkeletonPoint() {
            double angle = random.nextDouble() * 2 * Math.PI;
            int maxDistance = 0;
            Point bound;

            while (maxDistance <= minSkeletonDistanceFromCenter) {
                // Calculating the maximum distance from center to panel corner in direction of given angle
                bound = new Point((int) (0.5 * width * Math.cos(angle)), (int) (0.5 * height * Math.sin(angle)));
                maxDistance = (int) bound.distance(0, 0);
            }

            int distance = (int) (minSkeletonDistanceFromCenter +
                    (maxDistance - minSkeletonDistanceFromCenter) * random.nextDouble());
            return new PolarPoint(angle, distance);
        }

        private boolean skeletonPointIsValid(PolarPoint p, ArrayList<PolarPoint> otherPoints) {
            // Validating the distance from center
            if (p.distance <= minDistanceBetweenSkeletonPoints)
                return false;

            // Validating the angle between other points' vectors
            for (PolarPoint listPoint : otherPoints) {
                if (Math.abs(p.angle - listPoint.angle) < minAngleBetweenSkeletonLines)
                    return false;
            }
            return true;
        }

        private boolean skeletonIsValid() {
            return centerFitsGoodIntoPolygon();
        }

        private boolean centerFitsGoodIntoPolygon() {
            int shift = Math.min(width, height) / 10;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Point p = new Point(center.x + dx * shift, center.y + dy * shift);
                    if (!polygon.contains(p)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void draw(Graphics2D g) {
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(128, 128, 128));
            g.drawPolygon(polygon);
            int[] xPoints = polygon.xpoints;
            int[] yPoints = polygon.ypoints;
            for (int i = 0; i < xPoints.length; i++) {
                //g.fillOval(xPoints[i] - 5, yPoints[i] - 5, 10, 10);
                g.drawLine(xPoints[i], yPoints[i], center.x, center.y);
            }
        }

        private void generateSkeletonPolygon() {
            skeleton.polygon = getPolygonFromPolarPointsList(skeleton.points);
        }
    }
}


class PolarPoint implements Comparable<PolarPoint> {
    double angle;
    int distance;

    public PolarPoint(double angle, int distance) {
        this.angle = angle;
        this.distance = distance;
    }

    public Point getCartesianPoint() {
        return new Point((int) (Math.cos(angle) * distance), (int) (Math.sin(angle) * distance));
    }

    @Override
    public int compareTo(PolarPoint polarPoint) {
        return Double.compare(angle, polarPoint.angle);
    }
}