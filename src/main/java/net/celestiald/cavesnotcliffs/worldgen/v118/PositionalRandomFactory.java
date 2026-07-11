package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Produces deterministic random streams for a block position or stable string key. */
public interface PositionalRandomFactory {
    RandomSource at(int x, int y, int z);

    RandomSource fromHashOf(String name);

    default String parityConfigString() {
        StringBuilder builder = new StringBuilder();
        appendParityConfigString(builder);
        return builder.toString();
    }

    void appendParityConfigString(StringBuilder builder);
}
