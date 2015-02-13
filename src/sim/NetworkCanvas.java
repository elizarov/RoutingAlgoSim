package sim;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.GlyphVector;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * @author Roman Elizarov
 */
public class NetworkCanvas extends JComponent implements MouseListener, MouseMotionListener {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 700;

    private static final int DEFAULT_D = 10;

    private static final int TR = 0;
    private static final int TL = 1;
    private static final int BL = 2;
    private static final int BR = 3;

    private static final Color COLOR = Color.BLACK;

    private static final int LEGEND_TEXT_PAD = 5;
    private static final int LEGEND_LW = 80;

    private static final int NODE_R = 5;
    private static final int NODE_TEXT_PAD = 7;
    private static final int NODE_CLICK_DIST = 15;

    private static final int LINK_WIDTH = 1;
    private static final int BOLD_LINK_WIDTH = 3;

    private static final int FIN_L = 15;
    private static final int FIN_D = 5;

    private static final int MARK_D = 20;
    private static final int MARK_W = 5;

    private final NetworkUIModel model;

    private NID fromNode; // != null when painting link
    private NID dragNode; // != null when dragging node
    private boolean newFromNode; // true when painting link from new node
    private Point mousePos; // == null when outside

    public NetworkCanvas(NetworkUIModel model) {
        this.model = model;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBorder(new LineBorder(COLOR, 3));
        addMouseListener(this);
        addMouseMotionListener(this);
        model.addMsgsUpdateListener(this::repaint);
        model.addNodeUpdateListener(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintComponent((Graphics2D) g);
    }

    private void paintComponent(Graphics2D g) {
        // prepare list of nodes
        int n = model.getNodes().size();
        NID[] nis = model.getNIDs().toArray(new NID[n]); // all nodes, sorted!
        int[] uds = new int[n]; // prohibited directions
        // paint
        g.setColor(COLOR);
        paintLegend(g);
        paintLinks(g, nis, uds);
        paintNodes(g, nis, uds);

    }

    private void paintLegend(Graphics2D g) {
        drawStrings(g, new Point2D.Double(0, 0), model.getAlgo().getAlgoDescription(), false, BR, LEGEND_TEXT_PAD);
        Map<Integer,String> map = model.getAlgo().getLinkTypeLegend();
        ArrayList<String> ss = new ArrayList<>();
        ss.add("Legend:");
        ss.addAll(map.values());
        Point[] p = drawStrings(g, new Point2D.Double(getWidth(), getHeight()), ss, true, TL, LEGEND_TEXT_PAD);
        int i = 1;
        for (Integer type : map.keySet()) {
            drawLegendLink(g, p[i++], type);
        }
    }

    private void drawLegendLink(Graphics2D g, Point p, int flags) {
        drawLink(g, new Point(p.x - LEGEND_LW - LEGEND_TEXT_PAD, p.y), new Point(p.x - LEGEND_TEXT_PAD, p.y), flags);
    }

    private void paintLinks(Graphics2D g, NID[] nis, int[] uds) {
        Map<NID, Map<NID, ArrayList<String>>> linkStr = new HashMap<>();
        for (int i = 0; i < nis.length; i++) {
            NID in = nis[i];
            Point p = model.getPoint(in);
            AbstractNode<?> node = model.getNode(in);
            for (Map.Entry<NID, Integer> entry : node.getOutgoingLinks().entrySet()) {
                NID jn = entry.getKey();
                int d = entry.getValue();
                Point q = model.getPoint(jn);
                drawLink(g, p, q, node.getLinkFlags(jn));
                addToLinkStr(linkStr, in, jn, "" + d);
                int dir = getDir(p, q);
                uds[i] |= 1 << ((dir + 1) % 4);
                uds[Arrays.binarySearch(nis, jn)] |= 1 << ((dir + 3) % 4);
            }
        }
        if (fromNode != null && mousePos != null) {
            Point p = model.getPoint(fromNode);
            drawLink(g, p, mousePos, 0);
            int dir = getDir(p, mousePos);
            uds[Arrays.binarySearch(nis, fromNode)] |= 1 << ((dir + 1) % 4);
        }
        for (AbstractMsg msg : model.getMsgs()) {
            addToLinkStr(linkStr, msg.from, msg.to, msg.toShortString());
        }
        linkStr.forEach((in, m) -> m.forEach((jn, ss) -> {
            Point p = model.getPoint(in);
            Point q = model.getPoint(jn);
            int dir = getDir(p, q);
            boolean firstLineBold = model.getNode(in).getOutgoingLinks().containsKey(jn);
            drawStrings(g, new Point2D.Float((p.x + q.x) / 2f, (p.y + q.y) / 2f), ss, firstLineBold, dir, 1);
        }));
    }

    private void paintNodes(Graphics2D g, NID[] nis, int[] uds) {
        ArrayList<String> nodeStr = new ArrayList<>();
        for (int i = 0; i < nis.length; i++) {
            NID in = nis[i];
            Point p = model.getPoint(in);
            int dir = 0;
            for (int dd = 0; dd < 4; dd++)
                if ((uds[i] & (1 << dd)) == 0) {
                    dir = dd;
                    break;
                }
            nodeStr.clear();
            model.getNode(in).addNodeDataTo(nodeStr);
            drawStrings(g, p, nodeStr, true, dir, NODE_TEXT_PAD);
            g.fillOval(p.x - NODE_R, p.y - NODE_R, 2 * NODE_R, 2 * NODE_R);
        }
    }

    private void addToLinkStr(Map<NID, Map<NID, ArrayList<String>>> linkStr, NID in, NID jn, String s) {
        linkStr.computeIfAbsent(in, ($) -> new HashMap<>()).computeIfAbsent(jn, ($) -> new ArrayList<>()).add(s);
    }

    private void drawLink(Graphics2D g, Point p, Point q, int flags) {
        Stroke s = g.getStroke();
        int width = (flags & AbstractNode.LINK_BOLD) != 0 ? BOLD_LINK_WIDTH : LINK_WIDTH;
        g.setStroke(new BasicStroke(width));
        g.drawLine(p.x, p.y, q.x, q.y);
        double d = p.distance(q);
        if (d > FIN_D) {
            double vx = (p.x - q.x) / d;
            double vy = (p.y - q.y) / d;
            g.draw(new Line2D.Double(q.x, q.y, q.x + vx * FIN_L - vy * FIN_D, q.y + vy * FIN_L + vx * FIN_D));
            g.draw(new Line2D.Double(q.x, q.y, q.x + vx * FIN_L + vy * FIN_D, q.y + vy * FIN_L - vx * FIN_D));
        }
        g.setStroke(s);
        if ((flags & AbstractNode.LINK_ROUTE) != 0) {
            for (int i = MARK_D; i <= d - MARK_D; i += MARK_D) {
                double vx = (q.x - p.x) / d;
                double vy = (q.y - p.y) / d;
                double mx = p.x + i * vx;
                double my = p.y + i * vy;
                Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
                path.moveTo(mx + vx * MARK_W, my + vy * MARK_W);
                path.lineTo(mx - vy * MARK_W, my + vx * MARK_W);
                path.lineTo(mx + vy * MARK_W, my - vx * MARK_W);
                path.closePath();
                g.fill(path);
            }
        }
    }

    private Point[] drawStrings(Graphics2D g, Point2D p, java.util.List<String> ss, boolean firstLineBold, int d, int textPad) {
        double x = p.getX();
        double y = p.getY();
        double w = 0;
        double h = 0;
        int n = ss.size();
        Point[] result = new Point[n];
        GlyphVector[] gv = new GlyphVector[n];
        for (int i = 0; i < n; i++) {
            String s = ss.get(i);
            Font font = g.getFont();
            if (firstLineBold && i == 0)
                font = font.deriveFont(Font.BOLD);
            gv[i] = font.createGlyphVector(g.getFontRenderContext(), s);
            Rectangle2D b = gv[i].getLogicalBounds();
            w = Math.max(w, b.getWidth());
            h += b.getHeight();
            if (i == 0) {
                x -= b.getMinX();
                y -= b.getMinY();
            }
        }
        switch (d) {
            case TR:
                x += textPad;
                y -= h + textPad;
                break;
            case TL:
                x -= w + textPad;
                y -= h + textPad;
                break;
            case BL:
                x -= w + textPad;
                y += textPad;
                break;
            case BR:
                x += textPad;
                y += textPad;
        }
        for (int i = 0; i < n; i++) {
            g.drawGlyphVector(gv[i], (float) x, (float) y);
            Rectangle2D b = gv[i].getLogicalBounds();
            result[i] = new Point((int)x, (int)(y + b.getCenterY()));
            y += b.getHeight();
        }
        return result;
    }

    private int getDir(Point p, Point q) {
        if (q.x >= p.x) {
            return q.y <= p.y ? BR : BL;
        } else {
            return q.y <= p.y ? TR : TL;
        }
    }

    public NID findNodeByPoint(Point p) {
        for (NID i : model.getNIDs()) {
            if (model.getPoint(i).distance(p) < NODE_CLICK_DIST)
                return i;
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        NID i = findNodeByPoint(e.getPoint());
        if (i != null) {
            clickedNode(e, i, false);
            return;
        }
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                clickedNode(e, model.newNode(e.getPoint()), true);
                break;
            case MouseEvent.BUTTON3:
                cancelDrawing();
        }
    }

    private void clickedNode(MouseEvent e, NID i, boolean newNode) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                if (fromNode == null) {
                    fromNode = i;
                    newFromNode = newNode;
                    return;
                }
                if (i.equals(fromNode)) {
                    cancelDrawing();
                    return;
                }
                int d = model.getLink(fromNode, i);
                d = askForDistance("Distance " + fromNode + " -> " + i, d == DistUtil.INF ? DEFAULT_D : d);
                model.updateLink(fromNode, i, d);
                fromNode = null;
                repaint();
                break;
            case MouseEvent.BUTTON3:
                if (fromNode == null && !i.equals(NID.DEST))
                    model.removeNode(i);
                else
                    cancelDrawing();
        }
    }


    private void cancelDrawing() {
        // undo adding link (don't add)
        if (fromNode != null && newFromNode)
            model.removeNode(fromNode);
        fromNode = null;
        repaint();
    }

    private int askForDistance(String prompt, int d) {
        String s = JOptionPane.showInputDialog(prompt, "" + d);
        try {
            d = Integer.parseInt(s);
            return d <= 0 ? DistUtil.INF : d;
        } catch (NumberFormatException e) {
            return DistUtil.INF;
        }
    }

    private void updateMouse(MouseEvent e) {
        mousePos = e == null ? null : e.getPoint();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragNode = findNodeByPoint(e.getPoint());
        updateMouse(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateMouse(null);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragNode != null)
            model.setPoint(dragNode, e.getPoint());
        updateMouse(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMouse(e);
    }
}

