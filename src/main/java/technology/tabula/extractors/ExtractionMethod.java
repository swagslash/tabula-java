package technology.tabula.extractors;

public enum ExtractionMethod {
    Stream("stream"),
    Lattice("lattice");

    private final String name;

    ExtractionMethod(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
