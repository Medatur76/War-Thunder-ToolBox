import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Main extends Canvas implements Runnable {

    private Thread thread;
    private final Window window;
    private Dimension d;
    private BufferedImage bf;

    private final List<JsonObject> imgObject = new ArrayList<>();
    private List<JsonObject> drawObject = new ArrayList<>();

    private boolean running = false, updt = false, js = true;

    private static final float WIDTH = 640, HEIGHT = WIDTH / 12 * 9;
    private static float LWIDTH = 640, LHEIGHT = LWIDTH / 12 * 9;
    private static float CWIDTH = 640, CHEIGHT = CWIDTH / 12 * 9;
    public int zoom = 1;

    public synchronized void start() {
        thread = new Thread(this);
        thread.start();
        running = true;
    }

    public synchronized void stop() {
        try {
            thread.join();
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.requestFocus();
        long lastTime = System.nanoTime();
        double amountOfTicks = 5.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }
            if (running) {
                render();
                frames++;
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                System.out.println("FPS: " + frames);
                frames = 0;
            }
        }
        stop();
    }

    private void tick() {
        CWIDTH = (float) window.getWH().width;
        CHEIGHT = (float) window.getWH().height;
        if (LWIDTH != CWIDTH || LHEIGHT != CHEIGHT) {
            updt = true;
        }
        LWIDTH = CWIDTH;
        LHEIGHT = CHEIGHT;
        boolean b = false;
        try {
            URLConnection request = new URI("http://localhost:8111/map_info.json").toURL().openConnection();
            request.connect();
            b = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())).getAsJsonObject().get("valid").getAsBoolean();
        } catch (Exception _) {}
        try {
            if (b) {
                bf = Optional.ofNullable(ImageIO.read(new URI("http://localhost:8111/map.img?gen=-1").toURL())).orElse(ImageIO.read(Objects.requireNonNull(getClass().getResource("res/map.png"))));
                URLConnection request = new URI("http://localhost:8111/map_obj.json").toURL().openConnection();
                request.connect();

                JsonArray array = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())).getAsJsonArray();
                for (int i = 0; i < array.asList().size(); ) {
                    if (Objects.equals(array.get(i).getAsJsonObject().get("type").getAsString(), "ground_model") || Objects.equals(array.get(i).getAsJsonObject().get("type").getAsString(), "aircraft") || Objects.equals(array.get(i).getAsJsonObject().get("type").getAsString(), "bombing_point")) {
                        if (!imgObject.contains(array.get(i).getAsJsonObject())) {
                            imgObject.add(array.get(i).getAsJsonObject());
                            drawObject.add(array.get(i).getAsJsonObject());
                        }
                    }
                    i++;
                }
                if (js) js = false;
            } else if (!js) {
                genImage(true);
                js = true;
                bf = ImageIO.read(Objects.requireNonNull(getClass().getResource("res/map.png")));
            }
        } catch (IllegalStateException e) {
            if (!js && !b) {
                genImage(true);
                js = true;
            }
        } catch (URISyntaxException | IOException _) {}
    }

    private void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        Graphics2D g2d = (Graphics2D)g.create();

        g.setColor(Color.GRAY);
        g.fillRect(0, 0, (int) CWIDTH, (int) CHEIGHT);

        try {
            if (bf == null) bf = ImageIO.read(Objects.requireNonNull(getClass().getResource("res/map.png")));
        } catch (Exception _) {}

        if (d == null) {
            d = getDimensionKeepScale(bf.getWidth(), bf.getHeight(), CWIDTH, CHEIGHT);
        }
        if (updt) {
            d = getDimensionKeepScale(d.width, d.height, CWIDTH, CHEIGHT);
            updt = false;
        }

        int hi = 8;

        if (d.width <= 256) {
            hi = 2;
        } else if (d.width <= 512) {
            hi = 4;
        }

        d.height = d.height * zoom;
        d.width = d.width * zoom;

        g.drawImage(bf, 0, 0,d.width, d.height, null);

        for (int i = 0; i < drawObject.size();) {
            JsonObject object = drawObject.get(i).getAsJsonObject();
            g2d.setColor(Color.decode(object.get("color").getAsString()));
            Rectangle2D.Float rect = new Rectangle2D.Float(((float)d.width) * object.get("x").getAsFloat(), ((float)d.height) * object.get("y").getAsFloat(), hi * zoom, hi * zoom);
            g2d.fill(rect);
            i++;
        }

        /*g.setColor(Color.WHITE);

        g.setFont(new Font("arial", Font.BOLD, 52));

        g.drawString("Zoom: " + zoom, 0, 0);*/

        g.setColor(Color.BLUE);
        g.fillRect(170, 400, 300, 40);
        g.setColor(Color.BLACK);
        g.drawRect(170, 400, 300, 40);

        g.dispose();
        bs.show();

        drawObject.clear();
    }

    @NotNull
    public static Dimension getDimensionKeepScale(float owidth, float oheight, float wwidth, float wheight) {
        float oh = owidth / oheight;

        float fh = oheight, fw = owidth;

        boolean done = false;

        boolean lsub = false;

        for (float i = 1f; !done;) {
            if (i <= 0) {
                fh = 256;
                fw = 256;
                done = true;
            } else {
                float nheight = oheight + i;
                float nwidth = oh * nheight;
                if (nheight > wheight || nwidth > wwidth) {
                    i--;
                    lsub = true;
                    done = !((fh == oheight) && (fw == owidth));
                } else {
                    if (nheight % 1 == 0 && nwidth % 1 == 0) {
                        fh = nheight;
                        fw = nwidth;
                    }
                    if (lsub) i--;
                    else i++;
                }
            }
        }
        return new Dimension((int) fw,(int) fh);
    }

    public Main() {

        File output = new File("output");

        output.mkdir();

        Mouse mouse = new Mouse(this);
        //this.addMouseWheelListener(new Mouse(this));
        this.addMouseListener(mouse);
        window = new Window((int) WIDTH, (int) HEIGHT, "War Thunder ToolBox", this);
    }

    public static void main(String[] args) {
        new Main();
    }

    public void genImage(boolean auto) {
        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.drawImage(bf, 0, 0,image.getWidth(), image.getHeight(), null);

        for (int i = 0; i < imgObject.size();) {
            JsonObject object = imgObject.get(i).getAsJsonObject();
            g2d.setColor(Color.decode(object.get("color").getAsString()));
            Rectangle2D.Float rect = new Rectangle2D.Float(1024 * object.get("x").getAsFloat(), 1024 * object.get("y").getAsFloat(), 4, 4);
            g2d.fill(rect);
            i++;
        }

        g2d.dispose();

        String name = "output/" + LocalDateTime.now().toString().replace("T", "+").replace(":", "-");

        try {
            // Save as PNG
            File file = new File(name + ".png");
            ImageIO.write(image, "png", file);

            // Save as JPEG
            file = new File(name + ".jpg");
            ImageIO.write(image, "jpg", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (auto) {
            imgObject.clear();
        }
    }
}