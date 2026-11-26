import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    private static class WordCount {
        String word;
        int count;

        WordCount(String word, int count) {
            this.word = word;
            this.count = count;
        }
    }

    public static void main(String[] args) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    runTask1();
                    break;
                case "2":
                    runTask2();
                    break;
                case "0":
                    System.out.println("Завершення роботи.");
                    return;
                default:
                    System.out.println("Невірний вибір. Спробуйте ще раз.");
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("=== МЕНЮ ===");
        System.out.println("1 - Завдання 1: Підрахунок входжень слів та запис унікальних слів у файл");
        System.out.println("2 - Завдання 2: Підрахунок кількості слів у текстових файлах");
        System.out.println("0 - Вихід");
        System.out.print("Ваш вибір: ");
    }

    private static void runTask1() {
        try {
            System.out.print("Введіть шлях до файлу зі словами для пошуку: ");
            Path searchWordsPath = Paths.get(scanner.nextLine().trim());

            System.out.print("Введіть шлях до файлу з текстом: ");
            Path textFilePath = Paths.get(scanner.nextLine().trim());

            System.out.print("Введіть шлях до вихідного файлу для унікальних слів: ");
            Path outputFilePath = Paths.get(scanner.nextLine().trim());

            String searchContent = Files.readString(searchWordsPath, StandardCharsets.UTF_8);
            String textContent = Files.readString(textFilePath, StandardCharsets.UTF_8);

            List<String> targetWords = extractWords(searchContent);
            if (targetWords.isEmpty()) {
                System.out.println("Файл зі словами для пошуку порожній.");
                return;
            }

            List<String> allTextWords = extractWords(textContent);

            Map<String, Integer> frequencyMap = new HashMap<>();
            for (String word : allTextWords) {
                frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
            }

            List<WordCount> searchResults = new ArrayList<>();
            Set<String> uniqueTargets = new LinkedHashSet<>(targetWords);
            for (String target : uniqueTargets) {
                int count = frequencyMap.getOrDefault(target, 0);
                searchResults.add(new WordCount(target, count));
            }

            searchResults.sort(new Comparator<WordCount>() {
                @Override
                public int compare(WordCount a, WordCount b) {
                    int cmp = Integer.compare(b.count, a.count);
                    if (cmp != 0) {
                        return cmp;
                    }
                    return a.word.compareToIgnoreCase(b.word);
                }
            });

            System.out.println("\n--- Результат пошуку (за спаданням кількості входжень) ---");
            for (WordCount item : searchResults) {
                System.out.println(item.word + ": " + item.count);
            }

            Set<String> uniqueTextWords = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            uniqueTextWords.addAll(allTextWords);

            Files.write(outputFilePath, uniqueTextWords, StandardCharsets.UTF_8);
            System.out.println("\nВідсортований список слів тексту без дублікатів записано у: " + outputFilePath);

        } catch (IOException e) {
            System.out.println("Помилка роботи з файлом: " + e.getMessage());
        }
    }

    private static void runTask2() {
        System.out.println("Введіть шляхи до текстових файлів по одному (порожній рядок — завершити введення):");
        List<Path> files = new ArrayList<>();

        while (true) {
            System.out.print("Файл #" + (files.size() + 1) + ": ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                break;
            }
            files.add(Paths.get(line));
        }

        if (files.isEmpty()) {
            System.out.println("Файли не вказано.");
            return;
        }

        System.out.println("\n--- Кількість слів у файлах ---");
        for (Path filePath : files) {
            try {
                String content = Files.readString(filePath, StandardCharsets.UTF_8);
                int wordCount = countWordsWithSpaces(content);
                System.out.println(filePath.getFileName() + " — " + wordCount + " слів");
            } catch (IOException e) {
                System.out.println(filePath.getFileName() + " — помилка читання (" + e.getMessage() + ")");
            }
        }
    }

    private static List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = Pattern.compile("[\\p{L}\\p{N}_'-]+").matcher(text);
        while (matcher.find()) {
            words.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return words;
    }

    private static int countWordsWithSpaces(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        int totalWords = 0;
        String[] lines = content.split("\\R");
        for (String line : lines) {
            String trimmed = line.replaceAll("^[ ]+|[ ]+$", "");
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(" ");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        totalWords++;
                    }
                }
            }
        }
        return totalWords;
    }
}