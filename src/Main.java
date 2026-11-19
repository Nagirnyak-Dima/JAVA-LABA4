import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.print("Файл зі словами для пошуку: ");
        String wordsPath = sc.nextLine().trim().replace("\"", "");

        System.out.print("Файл з текстом: ");
        String textPath = sc.nextLine().trim().replace("\"", "");

        System.out.print("Вихідний файл для унікальних слів: ");
        String outPath = sc.nextLine().trim().replace("\"", "");

        List<String> searchWords = Files.readAllLines(Paths.get(wordsPath), StandardCharsets.UTF_8);
        String text = Files.readString(Paths.get(textPath), StandardCharsets.UTF_8).toLowerCase();

        // Прибираємо BOM, якщо файл збережено в UTF-8 з BOM
        text = text.replace("\uFEFF", "");

        String[] words = text.split("[^\\p{L}\\p{N}]+");

        Map<String, Integer> countMap = new HashMap<>();
        Set<String> uniqueWords = new TreeSet<>();

        for (String w : words) {
            if (!w.isEmpty()) {
                uniqueWords.add(w);
                countMap.put(w, countMap.getOrDefault(w, 0) + 1);
            }
        }

        List<String> resultLines = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String raw : searchWords) {
            String w = raw.replace("\uFEFF", "").trim().toLowerCase();
            if (!w.isEmpty() && !seen.contains(w)) {
                seen.add(w);
                int cnt = countMap.getOrDefault(w, 0);
                resultLines.add(w + " -> " + cnt);
            }
        }

        // Сортування за спаданням кількості входжень
        resultLines.sort((a, b) -> {
            int n1 = Integer.parseInt(a.substring(a.lastIndexOf(" ") + 1));
            int n2 = Integer.parseInt(b.substring(b.lastIndexOf(" ") + 1));
            return Integer.compare(n2, n1);
        });

        System.out.println("\nРезультати пошуку:");
        for (String line : resultLines) {
            System.out.println(line);
        }

        Files.write(Paths.get(outPath), uniqueWords, StandardCharsets.UTF_8);
        System.out.println("\nУнікальні відсортовані слова записано у файл.");
    }
}
