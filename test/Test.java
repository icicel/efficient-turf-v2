public class Test {
    public static void main(String[] args) throws Exception {
        String kml = EfficientTurf.getKML("example.kml");
        System.out.println(kml.length());
        String webKml = EfficientTurf.getWebKML("1sXlGZ7zc6IotVtMBEAtaHlTA4Uq2iA6q");
        System.out.println(webKml.length());
    }
}
