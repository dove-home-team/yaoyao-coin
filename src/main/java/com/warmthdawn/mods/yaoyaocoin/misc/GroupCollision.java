package com.warmthdawn.mods.yaoyaocoin.misc;

import java.util.ArrayList;
import java.util.List;

import com.warmthdawn.mods.yaoyaocoin.gui.CoinSlotGroup;

public class GroupCollision {
  private final Rectangle2i overlapRect;
  private final List<Rectangle2i> collisionRects;
  private final boolean isSingle;

  public GroupCollision(Rectangle2i overlapRect, List<Rectangle2i> collisionRects) {
    this.overlapRect = overlapRect;
    this.collisionRects = collisionRects;
    this.isSingle = collisionRects.size() == 1;
  }

  public static GroupCollision createSingle(Rectangle2i rect) {
    return new GroupCollision(rect, List.of(rect));
  }

  public static GroupCollision compute(CoinSlotGroup group, int slotSize) {
    return compute(group, slotSize, new Vector2i(group.getGroupX(), group.getGroupY()));
  }

  public static GroupCollision compute(CoinSlotGroup group, int slotSize, Vector2i pos) {
    if (group.getCollisionRects().size() == 1) {
      return createSingle(group.getCollisionRects().get(0).scaled(slotSize).translateInPlace(pos));
    }
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    List<Rectangle2i> collisionRects = new ArrayList<>(group.getCollisionRects().size());
    for (Rectangle2i rect : group.getCollisionRects()) {
      Rectangle2i actual = rect.scaled(slotSize).translateInPlace(pos);
      if (actual.getX() < minX) {
        minX = actual.getX();
      }
      if (actual.getY() < minY) {
        minY = actual.getY();
      }
      if (actual.getX1() > maxX) {
        maxX = actual.getX1();
      }
      if (actual.getY1() > maxY) {
        maxY = actual.getY1();
      }
      collisionRects.add(actual);
    }
    Rectangle2i overlapRect = new Rectangle2i(minX, minY, maxX - minX + 1, maxY - minY + 1);
    return new GroupCollision(overlapRect, collisionRects);
  }

  public GroupCollision expand(int amount) {
    List<Rectangle2i> newCollisionRects = new ArrayList<>(collisionRects.size());
    for (Rectangle2i rect : collisionRects) {
      newCollisionRects.add(rect.expand(amount));
    }
    return new GroupCollision(overlapRect.expand(amount), newCollisionRects);
  }

  public GroupCollision expandInPlace(int amount) {
    for (Rectangle2i rect : collisionRects) {
      rect.expandInPlace(amount);
    }
    overlapRect.expandInPlace(amount);
    return this;
  }

  public Rectangle2i getOverlapRect() {
    return overlapRect;
  }

  public List<Rectangle2i> getCollisionRects() {
    return collisionRects;
  }

  public GroupCollision translated(Vector2i offset) {
    List<Rectangle2i> newCollisionRects = new ArrayList<>(collisionRects.size());
    for (Rectangle2i rect : collisionRects) {
      newCollisionRects.add(rect.translated(offset));
    }
    return new GroupCollision(overlapRect.translated(offset), newCollisionRects);
  }

  @SuppressWarnings("UnusedReturnValue")
  public GroupCollision translateInPlace(Vector2i offset) {
    for (Rectangle2i rect : collisionRects) {
      rect.translateInPlace(offset);
    }
    overlapRect.translateInPlace(offset);
    return this;
  }

  public boolean intersects(Rectangle2i rect) {
    if (this.isSingle) {
      return collisionRects.get(0).intersects(rect);
    }
    if (!overlapRect.intersects(rect)) {
      return false;
    }
    for (Rectangle2i collisionRect : collisionRects) {
      if (collisionRect.intersects(rect)) {
        return true;
      }
    }
    return false;
  }

  public boolean intersects(GroupCollision collision) {
    if (this.isSingle) {
      return collision.intersects(collisionRects.get(0));
    }
    if (!collision.intersects(overlapRect)) {
      return false;
    }
    for (Rectangle2i rect : collision.getCollisionRects()) {
      if (this.intersects(rect)) {
        return true;
      }
    }
    return false;
  }
}
