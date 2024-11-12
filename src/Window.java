import javax.swing.*;
import java.awt.*;

public class Window {

    private JFrame frame;

    public Window(int width, int height, String title, Main main) {
        frame = new JFrame(title);

        frame.setSize(new Dimension(width, height));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.add(main);
        frame.setVisible(true);
        main.start();
    }

    public Dimension getWH() {
        return new Dimension(frame.getWidth(), frame.getHeight());
    }
}
