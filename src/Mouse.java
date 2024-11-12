import java.awt.event.*;

public class Mouse extends MouseAdapter implements MouseWheelListener {

    private final Main main;

    public Mouse(Main main) {
        this.main = main;
    }

    /*@Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(e.getPreciseWheelRotation() + "\n" + e.getScrollAmount());
        main.zoom += (int) -e.getPreciseWheelRotation();
    }*/

    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if (moveOver(x, y,(float) 170,(float) 400,(float) 300,(float) 40)) {
            main.genImage(false);
        }
    }

    private boolean moveOver(int mx, int my, float x, float y, float width, float height) {
        return (mx > x && mx < x + width) && (my > y && my < y + height);
    }
}
