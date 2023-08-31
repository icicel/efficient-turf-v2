public class Test {
    public static void main(String[] args) throws Exception {
        String localKml = EfficientTurf.getKML("example.kml");
        System.out.println(localKml.length());
        String webKml = EfficientTurf.getWebKML("1sXlGZ7zc6IotVtMBEAtaHlTA4Uq2iA6q");
        System.out.println(webKml.length());

        KMLParser kml = new KMLParser(localKml);
    }
}
