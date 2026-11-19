import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;class WordCountPerFileApp {

    static class FileWordStat {
        String fileName;
        int wordCount;

        FileWordStat(String fileName, int wordCount) {
            this.fileName = fileName;
            this.wordCount = wordCount;
        }
    }

    private static String cleanPath(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replace("\"", "");
    }

    private static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    // Зауваження 1 і 2: шлях з консолі; якщо файлу немає — повідомлення і повторний запит
    private static Path promptExistingInputFile(BufferedReader console, String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String input = console.readLine();
            if (input == null) {
                return null;
            }
            String cleaned = cleanPath(input);
            if (cleaned.isEmpty()) {
                System.out.println("Помилка: шлях не може бути порожнім. Спробуйте ще раз.");
                continue;
            }

            Path path = Paths.get(cleaned);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return path;
            }
            System.out.println("Помилка: вхідний файл не знайдено або це не файл. Введіть шлях ще раз.");
        }
    }

    // Зауваження 3: якщо вихідного файлу немає — повідомлення і запит на створення
    private static Path promptOutputFile(BufferedReader console, String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String input = console.readLine();
            if (input == null) {
                return null;
            }
            String cleaned = cleanPath(input);
            if (cleaned.isEmpty()) {
                System.out.println("Помилка: шлях вихідного файлу не може бути порожнім.");
                continue;
            }

            Path path = Paths.get(cleaned);
            if (Files.exists(path)) {
                if (Files.isRegularFile(path)) {
                    return path;
                }
                System.out.println("Помилка: за вказаним шляхом знаходиться папка, а не файл.");
                continue;
            }

            System.out.print("Вихідний файл відсутній. Створити його? (y/n): ");
            String answer = console.readLine();
            if (answer != null && (answer.trim().equalsIgnoreCase("y")
                    || answer.trim().equalsIgnoreCase("yes")
                    || answer.trim().equalsIgnoreCase("так"))) {
                Path parent = path.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(path);
                System.out.println("Створено новий файл: " + path.toAbsolutePath());
                return path;
            } else {
                System.out.println("Вихідний файл не створено. Вкажіть інший шлях або спробуйте ще раз.");
            }
        }
    }

    // Зауваження 4: вибір між перезаписом і дописуванням у кінець існуючого файлу
    private static boolean promptAppendMode(BufferedReader console, Path outputFile) throws IOException {
        if (Files.exists(outputFile) && Files.size(outputFile) > 0) {
            while (true) {
                System.out.print("Файл уже існує і містить дані. Перезаписати (o) чи дописати в кінець (a)? [o/a]: ");
                String choice = console.readLine();
                if (choice == null) {
                    return false;
                }
                String mode = choice.trim().toLowerCase(Locale.ROOT);
                if (mode.equals("a") || mode.equals("append")) {
                    return true;
                }
                if (mode.equals("o") || mode.equals("overwrite")) {
                    return false;
                }
                System.out.println("Введіть 'o' для перезапису або 'a' для дописування в кінець.");
            }
        }
        return false;
    }

    // Послідовне буферизоване читання файлу та підрахунок слів
    private static int countWordsInFileSequentially(Path filePath) throws IOException {
        int totalWords = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = stripBom(line);
                    firstLine = false;
                }
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    String[] words = trimmed.split("\\s+");
                    totalWords += words.length;
                }
            }
        }

        return totalWords;
    }

    public static void main(String[] args) {
        // Зауваження 5: буферизований консольний ввід-вивід
        try (BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            List<FileWordStat> stats = new ArrayList<>();

            System.out.println("=== Підрахунок кількості слів у текстових файлах ===");
            while (true) {
                Path inputFile = promptExistingInputFile(
                        console, "Введіть шлях до текстового файлу (або Enter для завершення вводу): ");

                if (inputFile == null) {
                    break;
                }

                int count = countWordsInFileSequentially(inputFile);
                String fileName = inputFile.getFileName().toString();
                stats.add(new FileWordStat(fileName, count));

                // Вивід назви файлу і кількості слів одразу на екран
                System.out.println("Файл: " + fileName + " | Кількість слів: " + count);

                System.out.print("Бажаєте ввести ще один текстовий файл? (y/n): ");
                String more = console.readLine();
                if (more == null || !(more.trim().equalsIgnoreCase("y")
                        || more.trim().equalsIgnoreCase("yes")
                        || more.trim().equalsIgnoreCase("так"))) {
                    break;
                }
            }

            System.out.println("\n--- Підсумок по всіх файлах ---");
            for (FileWordStat item : stats) {
                System.out.println(item.fileName + " -> " + item.wordCount);
            }

            System.out.print("\nБажаєте зберегти результати у вихідний файл? (y/n): ");
            String saveChoice = console.readLine();
            if (saveChoice != null && (saveChoice.trim().equalsIgnoreCase("y")
                    || saveChoice.trim().equalsIgnoreCase("yes")
                    || saveChoice.trim().equalsIgnoreCase("так"))) {

                Path outputFile = promptOutputFile(console, "Введіть шлях до вихідного файлу: ");
                if (outputFile != null) {
                    boolean append = promptAppendMode(console, outputFile);
                    StandardOpenOption option = append
                            ? StandardOpenOption.APPEND
                            : StandardOpenOption.TRUNCATE_EXISTING;

                    try (BufferedWriter writer = Files.newBufferedWriter(
                            outputFile, StandardCharsets.UTF_8, option)) {
                        for (FileWordStat item : stats) {
                            writer.write(item.fileName + " -> " + item.wordCount);
                            writer.newLine();
                        }
                    }
                    System.out.println("Результати записано у файл: " + outputFile.toAbsolutePath());
                }
            }

        } catch (IOException e) {
            System.out.println("Помилка вводу-виводу: " + e.getMessage());
        }
    }
}
