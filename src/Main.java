import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.sql.Time;
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

    private File outputPic, outputVid, map;
    private FileWriter picWriter, vidWriter;

    private final List<JsonObject> drawObject = new ArrayList<>();
    int f = 0;

    private volatile boolean running = false, js = true, b = false, rendering = true;

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
        rendering = true;
        CWIDTH = (float) window.getWH().width;
        CHEIGHT = (float) window.getWH().height;
        if (LWIDTH != CWIDTH || LHEIGHT != CHEIGHT) {
            d = getDimensionKeepScale(d.width, d.height, CWIDTH, CHEIGHT);
        }
        LWIDTH = CWIDTH;
        LHEIGHT = CHEIGHT;
        try {
            URLConnection request = new URI("http://localhost:8111/map_info.json").toURL().openConnection();
            request.connect();
            b = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())).getAsJsonObject().get("valid").getAsBoolean();
        } catch (Exception _) {}
        try {
            if (b) {
                if (!(outputPic.exists())) outputPic.createNewFile();
                if (!(outputVid.exists())) outputVid.createNewFile();
                bf = Optional.ofNullable(ImageIO.read(new URI("http://localhost:8111/map.img?gen=-1").toURL())).orElse(ImageIO.read(Objects.requireNonNull(getClass().getResource("res/map.png"))));
                if (!(map.exists())) {map.createNewFile();ImageIO.write(bf, "png", map);}
                URLConnection request = new URI("http://localhost:8111/map_obj.json").toURL().openConnection();
                request.connect();
                JsonArray array = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())).getAsJsonArray();
                for (int i = 0; i < array.asList().size();) {
                    if (Objects.equals(array.get(i).getAsJsonObject().get("type").getAsString(), "ground_model") || Objects.equals(array.get(i).getAsJsonObject().get("type").getAsString(), "aircraft") || Objects.equals(array.get(i).getAsJsonObject().get("type").getAsString(), "bombing_point")) {
                        drawObject.add(array.get(i).getAsJsonObject());
                    }
                    i++;
                }
                if (js) js = false;
            } else if (!js) {
                genImage(true);
                js = true;
                bf = ImageIO.read(getClass().getResource("res/map.png"));
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

        if (drawObject.isEmpty() && !js) {
            tick();
        }

        try {
            if (bf == null) bf = ImageIO.read(getClass().getResource("res/map.png"));
        } catch (Exception e) {e.printStackTrace();}

        Graphics g = bs.getDrawGraphics();

        Graphics2D g2d = (Graphics2D)g.create();

        g.setColor(Color.GRAY);
        g.fillRect(0, 0, (int) CWIDTH, (int) CHEIGHT);

        if (d == null) {
            d = getDimensionKeepScale(bf.getWidth(), bf.getHeight(), CWIDTH, CHEIGHT);
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

        StringBuilder yello = null;

        if (b) yello = new StringBuilder("\t\"" + f + "\": [");

        for (int i = 0; i < drawObject.size();) {
            JsonObject object = drawObject.get(i).getAsJsonObject();
            WarThunderObject wto = new WarThunderObject(WarThunderObject.Type.fromString(object.get("type").getAsString()), object.get("color").getAsString(), object.get("blink").getAsInt(), WarThunderObject.Icon.fromString(object.get("icon").getAsString()), object.get("x").getAsFloat(), object.get("y").getAsFloat());
            try {
                if (f == 0) picWriter.append("\t\"").append(String.valueOf(wto)).append("\"\n");
                else picWriter.append(",\t\"").append(String.valueOf(wto)).append("\"\n");
            } catch (Exception e) {e.printStackTrace();}
            g2d.setColor(wto.getColor());
            Rectangle2D.Float rect = new Rectangle2D.Float(((float)d.width) * wto.getX(), ((float)d.height) * wto.getY(), hi * zoom, hi * zoom);
            g2d.fill(rect);
            if (i == 0 && b) yello.append("\n\t\t").append("\"").append(wto).append("\"");
            else if (b) yello.append(",\n\t\t").append("\"").append(wto).append("\"");
            i++;
        }

        if (b) {yello.append("\n\t],\n"); try {vidWriter.append(yello.toString());} catch (Exception e) {e.printStackTrace();} f++;}

        g.setColor(Color.BLUE);
        g.fillRect(170, 400, 300, 40);
        g.setColor(Color.BLACK);
        g.drawRect(170, 400, 300, 40);

        g.dispose();
        bs.show();

        drawObject.clear();
        rendering = false;
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

        if (!(output.exists())) output.mkdir();

        map = new File("output/map.png");

        outputPic = new File("output/photoOutput.json");
        outputVid = new File("output/videoOutput.json");

        try {
            picWriter = new FileWriter(outputPic);
            picWriter.append("[\n");
            vidWriter = new FileWriter(outputVid);
            vidWriter.append("{\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Mouse mouse = new Mouse(this);
        //this.addMouseWheelListener(new Mouse(this));
        this.addMouseListener(mouse);
        window = new Window((int) WIDTH, (int) HEIGHT, "War Thunder ToolBox", this);
    }

    public static void main(String[] args) {
        new Main();
    }

    public void genImage(boolean auto) {
        while (rendering) {
            Thread.onSpinWait();
        }
        System.out.println(LocalTime.now());
        BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {g2d.drawImage(ImageIO.read(map), 0, 0,image.getWidth(), image.getHeight(), null);} catch (Exception e) {e.printStackTrace();}

        JsonArray array = null;

        try {picWriter.append("]");array = JsonParser.parseReader(new FileReader("output/wphotoOutput.json")).getAsJsonArray();} catch (Exception e) {e.printStackTrace();}

        for (int i = 0; i < array.size();) {
            System.out.println(((((float) i) / ((float) array.size()))*100) + "%");
            WarThunderObject object = WarThunderObject.fromString(array.get(i).getAsString());
            g2d.setColor(object.getColor());
            Rectangle2D.Float rect = new Rectangle2D.Float(1024 * object.getX(), 1024 * object.getY(), 4, 4);
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
            //PrintWriter pw = new PrintWriter(picWriter, false);
            //pw.flush();
            //pw.close();
            //try {picWriter.close();} catch (Exception e) {e.printStackTrace();}
        }

        try {
            final Rational framerate = Rational.make(1, 50);

            final Muxer muxer = Muxer.make(name + ".mp4", null, null);

            final MuxerFormat format = muxer.getFormat();
            final Codec codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());

            Encoder encoder = Encoder.make(codec);

            encoder.setWidth(1024);
            encoder.setHeight(1024);

            final PixelFormat.Type pixelformat = PixelFormat.Type.PIX_FMT_YUV420P;
            encoder.setPixelFormat(pixelformat);
            encoder.setTimeBase(framerate);

            if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER)) encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);

            encoder.open(null, null);
            muxer.addNewStream(encoder);
            muxer.open(null, null);

            MediaPictureConverter converter = null;
            final MediaPicture picture = MediaPicture.make(encoder.getWidth(), encoder.getHeight(), pixelformat);
            picture.setTimeBase(framerate);

            final MediaPacket packet = MediaPacket.make();

            JsonObject object = null;

            try {vidWriter.append("}");object = JsonParser.parseReader(new FileReader("output/wvideoOutput.json")).getAsJsonObject();} catch (Exception e) {e.printStackTrace();}

            for (int i = 0; i < object.size(); i++) {
                System.out.println(((((float) i) / ((float) array.size()))*100) + "%");
                final BufferedImage frame = new BufferedImage(1024, 1024, BufferedImage.TYPE_3BYTE_BGR);
                final Graphics2D gd = (Graphics2D) frame.getGraphics().create();
                final BufferedImage icons = new BufferedImage(1024, 1024, BufferedImage.TYPE_3BYTE_BGR);
                makeFrame(object.get(i + "").getAsJsonArray(), icons);

                gd.drawImage(ImageIO.read(map), 0, 0,image.getWidth(), image.getHeight(), null);
                gd.drawImage(icons, 0, 0, frame.getWidth(), frame.getHeight(), null);

                gd.dispose();

                if (converter == null)
                    converter = MediaPictureConverterFactory.createConverter(frame, picture);
                converter.toPicture(picture, frame, i);

                do {
                    encoder.encode(packet, picture);
                    if (packet.isComplete())
                        muxer.write(packet, false);
                } while (packet.isComplete());
            }

            do {
                encoder.encode(packet, null);
                if (packet.isComplete())
                    muxer.write(packet, false);
            } while (packet.isComplete());

            muxer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (auto) {
            //PrintWriter pw2 = new PrintWriter(vidWriter, false);
            //pw2.flush();
            //pw2.close();
            //try {vidWriter.close();} catch (Exception e) {e.printStackTrace();}
            f = 0;
            map.delete(); //needs to be fixed
        }
        System.out.println("done!");
        System.out.println(LocalTime.now());
    }

    public static void makeFrame(JsonArray array, BufferedImage image) {
        Graphics g = image.getGraphics();
        Graphics2D g2d = (Graphics2D) g.create();
        for (int i = 0; i < array.size();) {
            WarThunderObject object = WarThunderObject.fromString(array.get(i).getAsString());
            g2d.setColor(object.getColor());
            Rectangle2D.Float r2d = new Rectangle2D.Float(1024 * object.getX(), 1024 * object.getY(), 4, 4);
            g2d.fill(r2d);
            i++;
        }

    }
}