public class TestCsv {
    public static void main(String[] args) {
        String linea = "2026-01-01;Año Nuevo;NACIONAL;   ";
        String[] partes = linea.split(";");
        System.out.println("Length: " + partes.length);
        for(int i=0; i<partes.length; i++) {
            System.out.println("[" + i + "]: \"" + partes[i] + "\" (trimmed: \"" + partes[i].trim() + "\")");
        }
        String ciudad = partes[3].trim();
        System.out.println("Ciudad: \"" + ciudad + "\" isBlank: " + ciudad.isBlank());
    }
}
