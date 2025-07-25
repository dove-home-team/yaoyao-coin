package com.warmthdawn.mods.yaoyaocoin.misc;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Block {
    boolean[][] matrix;
    int posX, posY;

    public Block(boolean[][] matrix, int posX, int posY) {
        this.matrix = matrix;
        this.posX = posX;
        this.posY = posY;
    }

    public boolean intersects(Block other) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j] && other.isInside(i + posX, j + posY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canAdsorb(Block other) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                if (matrix[i][j] && other.isNear(i + posX, j + posY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInside(int x, int y) {
        x -= posX;
        y -= posY;
        return x >= 0 && y >= 0 && x < matrix.length && y < matrix[0].length && matrix[x][y];
    }

    public boolean isNear(int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (isInside(x + i, y + j)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final Vector2i[] DIRECTIONS = {
        new Vector2i(1, 0),
        new Vector2i(-1, 0),
        new Vector2i(0, 1),
        new Vector2i(0, -1)
    };

    public static Vector2i moveBlocks(Block block1, Block block2, Predicate<Vector2i> predicate) {
        ArrayDeque<Vector2i> queue = new ArrayDeque<>();
        Set<Vector2i> visited = new HashSet<>();

        queue.offer(new Vector2i(0, 0));
        visited.add(new Vector2i(0, 0));

        while (!queue.isEmpty()) {
            Vector2i offset = queue.poll();
            block1.posX += offset.getX();
            block1.posY += offset.getY();

            if (!block1.intersects(block2) && block1.canAdsorb(block2) && predicate.test(new Vector2i(offset.getX(), offset.getY()))) {
                return offset;
            }

            for (Vector2i direction : DIRECTIONS) {
                Vector2i newPos = offset.add(direction);

                if(newPos.lengthManhattan() > 10) {
                    continue;
                }

                if (!visited.contains(newPos)) {
                    queue.offer(newPos);
                    visited.add(newPos);
                }
            }

            block1.posX -= offset.getX();
            block1.posY -= offset.getY();
        }

        return null;
    }

    public int getX() {
        return posX;
    }

    public int getY() {
        return posY;
    }
}
