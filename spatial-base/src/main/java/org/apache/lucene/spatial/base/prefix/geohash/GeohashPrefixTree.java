package org.apache.lucene.spatial.base.prefix.geohash;

import org.apache.lucene.spatial.base.context.SpatialContext;
import org.apache.lucene.spatial.base.prefix.Node;
import org.apache.lucene.spatial.base.prefix.SpatialPrefixTree;
import org.apache.lucene.spatial.base.shape.Point;
import org.apache.lucene.spatial.base.shape.Shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A SpatialPrefixGrid based on Geohashes.  Uses {@link GeohashUtils} to do all the geohash work.
 */
public class GeohashPrefixTree extends SpatialPrefixTree {

  public GeohashPrefixTree(SpatialContext ctx, int maxLevels) {
    super(ctx, maxLevels);
    int MAXP = getMaxLevelsPossible();
    if (maxLevels <= 0 || maxLevels > MAXP)
      throw new IllegalArgumentException("maxLen must be [1-"+MAXP+"] but got "+ maxLevels);
  }

  /** Any more than this and there's no point (double lat & lon are the same). */
  public static int getMaxLevelsPossible() {
    return GeohashUtils.MAX_PRECISION;
  }

  @Override
  public int getLevelForDistance(double dist) {
    final int level = GeohashUtils.lookupHashLenForWidthHeight(dist, dist);
    return Math.max(Math.min(level, maxLevels), 1);
  }

  @Override
  public Node getNode(Point p, int level) {
    return new GhCell(GeohashUtils.encode(p.getY(),p.getX(), level));//args are lat,lon (y,x)
  }

  @Override
  public Node getNode(String token) {
    return new GhCell(token);
  }

  @Override
  public Node getNode(byte[] bytes, int offset, int len) {
    return new GhCell(bytes, offset, len);
  }

  @Override
  public List<Node> getNodes(Shape shape, int detailLevel, boolean inclParents) {
    return shape instanceof Point ? super.getNodesAltPoint((Point) shape, detailLevel, inclParents) :
        super.getNodes(shape, detailLevel, inclParents);
  }

  class GhCell extends Node {
    GhCell(String token) {
      super(GeohashPrefixTree.this, token);
    }

    GhCell(byte[] bytes, int off, int len) {
      super(GeohashPrefixTree.this, bytes, off, len);
    }

    @Override
    public void reset(byte[] bytes, int off, int len) {
      super.reset(bytes, off, len);
      shape = null;
    }

    @Override
    public Collection<Node> getSubCells() {
      String[] hashes = GeohashUtils.getSubGeohashes(getGeohash());//sorted
      List<Node> cells = new ArrayList<Node>(hashes.length);
      for (String hash : hashes) {
        cells.add(new GhCell(hash));
      }
      return cells;
    }

    @Override
    public int getSubCellsSize() {
      return 32;//8x4
    }

    @Override
    public Node getSubCell(Point p) {
      return GeohashPrefixTree.this.getNode(p,getLevel()+1);//not performant!
    }

    private Shape shape;//cache

    @Override
    public Shape getShape() {
      if (shape == null) {
        shape = GeohashUtils.decodeBoundary(getGeohash(), ctx);
      }
      return shape;
    }

    @Override
    public Point getCenter() {
      return GeohashUtils.decode(getGeohash(), ctx);
    }

    private String getGeohash() {
      return getTokenString();
    }

  }

}