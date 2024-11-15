import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class WarThunderObject {

    public enum Type {
        Airfield("airfield"),
        GroundModel("ground_model"),
        CaptureZone("capture_zone"),
        RespawnBaseTank("respawn_base_tank"),
        RespawnBaseFighter("respawn_base_fighter");

        private final String con;

        Type(String eq) {
            this.con = eq;
        }

        public static Type fromString(String type) {
            return switch (type) {
                case "airfield" -> Airfield;
                case "capture_zone" -> CaptureZone;
                case "respawn_base_tank" -> RespawnBaseTank;
                case "respawn_base_fighter" -> RespawnBaseFighter;
                default -> GroundModel;
            };
        }

        @Override
        public String toString() {
            return con;
        }
    }

    public enum Icon {
        Player,
        LightTank,
        MediumTank,
        HeavyTank,
        CaptureZone,
        AirDefence;

        public static Icon fromString(String icon) {
            return switch (icon) {
                case "Player" -> Player;
                case "LightTank" -> LightTank;
                case "HeavyTank" -> HeavyTank;
                case "capture_zone" -> CaptureZone;
                case "Airdefence" -> AirDefence;
                default -> MediumTank;
            };
        }

    }

    private final Type type;
    private final String color;
    private final int blink;
    private final Icon icon;
    private final float x, y;

    public WarThunderObject(Type type, String color, int blink, Icon icon, float x, float y) {
        this.type = type;
        this.color = color;
        this.blink = blink;
        this.icon = icon;
        this.x = x;
        this.y = y;
    }

    public Type getType() {
        return type;
    }

    public Color getColor() {
        return Color.decode(color);
    }

    public int getBlink() {
        return blink;
    }

    public Icon getIcon() {
        return icon;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public String toString() {
        return "{" +
                "type=" + type +
                ", color=" + color +
                ", blink=" + blink +
                ", icon=" + icon +
                ", x=" + x +
                ", y=" + y +
                '}';
    }

    public static @NotNull WarThunderObject fromString(String warThunderObject) {
        String[] list = warThunderObject.split(", ");
        Type type = Type.GroundModel;
        String color = "#FFFFFF";
        int blink = 0;
        Icon icon = Icon.MediumTank;
        float x = 0, y = 0;
        for (String hi : list) {
            String[] hallo = hi.split("=");
            if (hallo[0].contains("{")) hallo[0] = hallo[0].split("\\{")[1];
            if (hallo[1].contains("}")) hallo[1] = hallo[1].replace("}", "");
            switch (hallo[0]) {
                case "type":
                    type = Type.fromString(hallo[1]);
                    break;
                case "color":
                    color = hallo[1];
                    break;
                case "blink":
                    blink = Integer.decode(hallo[1]);
                    break;
                case "icon":
                    icon = Icon.fromString(hallo[1]);
                    break;
                case "x":
                    x = Float.parseFloat(hallo[1]);
                    break;
                case "y":
                    y = Float.parseFloat(hallo[1]);
                    break;
            }
        }
        return new WarThunderObject(type, color, blink, icon, x, y);
    }
}
