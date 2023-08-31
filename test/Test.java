public class Test {
    public static void main(String[] args) throws Exception {
        KML localKml = EfficientTurf.getKML("example.kml");
        System.out.println(localKml.asString().length());
        KML webKml = EfficientTurf.getWebKML("1sXlGZ7zc6IotVtMBEAtaHlTA4Uq2iA6q");
        System.out.println(webKml.asString().length());

        KMLParser kml = new KMLParser(localKml);
    }
}
