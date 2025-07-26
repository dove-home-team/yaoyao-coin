package com.warmthdawn.mods.yaoyaocoin.kubejs;

import com.warmthdawn.mods.yaoyaocoin.config.LayoutArea;

public interface CoinLayoutAreaJS {
    LayoutArea topLeft = LayoutArea.TOP_LEFT;
    LayoutArea topRight = LayoutArea.TOP_RIGHT;
    LayoutArea bottomLeft = LayoutArea.BOTTOM_LEFT;
    LayoutArea bottomRight = LayoutArea.BOTTOM_RIGHT;
    LayoutArea topCenter = LayoutArea.TOP_CENTER;
    LayoutArea bottomCenter = LayoutArea.BOTTOM_CENTER;
    LayoutArea centerLeft = LayoutArea.CENTER_LEFT;
    LayoutArea centerRight = LayoutArea.CENTER_RIGHT;

    LayoutArea TOP_LEFT = LayoutArea.TOP_LEFT;
    LayoutArea TOP_RIGHT = LayoutArea.TOP_RIGHT;
    LayoutArea BOTTOM_LEFT = LayoutArea.BOTTOM_LEFT;
    LayoutArea BOTTOM_RIGHT = LayoutArea.BOTTOM_RIGHT;
    LayoutArea TOP_CENTER = LayoutArea.TOP_CENTER;
    LayoutArea BOTTOM_CENTER = LayoutArea.BOTTOM_CENTER;
    LayoutArea CENTER_LEFT = LayoutArea.CENTER_LEFT;
    LayoutArea CENTER_RIGHT = LayoutArea.CENTER_RIGHT;


    static LayoutArea of(Object obj) {
        if (obj instanceof LayoutArea) {
            return (LayoutArea) obj;
        }

        String name = obj.toString();
        switch (name) {
            case "topLeft":
                return LayoutArea.TOP_LEFT;
            case "topRight":
                return LayoutArea.TOP_RIGHT;
            case "bottomLeft":
                return LayoutArea.BOTTOM_LEFT;
            case "bottomRight":
                return LayoutArea.BOTTOM_RIGHT;
            case "topCenter":
                return LayoutArea.TOP_CENTER;
            case "bottomCenter":
                return LayoutArea.BOTTOM_CENTER;
            case "centerLeft":
                return LayoutArea.CENTER_LEFT;
            case "centerRight":
                return LayoutArea.CENTER_RIGHT;
            default:
                String upper = name.toUpperCase();
                try {
                    return LayoutArea.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    return LayoutArea.INVALID;
                }
        }
    }
}
