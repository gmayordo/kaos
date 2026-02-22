public class TestSplit {
    public static void main(String[] args) {
        String s = "2026-01-01;Año Nuevo;NACIONAL;";
        String[] parts = s.split(";");
        System.out.println("Length: " + parts.length);
        for(int i=0; i<parts.length; i++) {
            System.out.println("[" + i + "]: \"" + parts[i] + "\"");
        }
    }
}
