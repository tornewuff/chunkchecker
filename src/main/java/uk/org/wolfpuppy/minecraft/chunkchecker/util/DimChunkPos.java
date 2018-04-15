package uk.org.wolfpuppy.minecraft.chunkchecker.util;

import net.minecraft.util.math.ChunkPos;

import java.util.Objects;

public class DimChunkPos {
    public final int dim;
    public final int x;
    public final int z;

    public DimChunkPos(int dim, int x, int z) {
        this.dim = dim;
        this.x = x;
        this.z = z;
    }

    public DimChunkPos(int dim, ChunkPos pos) {
        this.dim = dim;
        this.x = pos.x;
        this.z = pos.z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimChunkPos that = (DimChunkPos) o;
        return dim == that.dim &&
                x == that.x &&
                z == that.z;
    }

    @Override
    public int hashCode() {

        return Objects.hash(dim, x, z);
    }

    public String toString() {
        return "[" + this.dim + "; " + this.x + ", " + this.z + "]";
    }
}
