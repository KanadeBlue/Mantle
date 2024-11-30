package slimeknights.mantle.command.client;

import net.minecraft.util.math.vector.Matrix4f;

class MatrixHelper {
  private static final int M_00 = 0;
  private static final int M_11 = 5;
  private static final int M_22 = 10;
  private static final int M_03 = 3;
  private static final int M_13 = 7;
  private static final int M_23 = 11;
  private static final int M_33 = 15;

  /** Copy of the 1.18+ version of {@link Matrix4f#orthographic(float, float, float, float)} as 1.16 is missing the ability to negate height */
  public static Matrix4f orthographic(float pMinX, float pMaxX, float pMinY, float pMaxY, float pMinZ, float pMaxZ) {
    float[] values = new float[16];
    float f = pMaxX - pMinX;
    float f1 = pMinY - pMaxY;
    float f2 = pMaxZ - pMinZ;
    values[M_00] = 2.0F / f;
    values[M_11] = 2.0F / f1;
    values[M_22] = -2.0F / f2;
    values[M_03] = -(pMaxX + pMinX) / f;
    values[M_13] = -(pMinY + pMaxY) / f1;
    values[M_23] = -(pMaxZ + pMinZ) / f2;
    values[M_33] = 1.0F;
    return new Matrix4f(values);
  }
}
