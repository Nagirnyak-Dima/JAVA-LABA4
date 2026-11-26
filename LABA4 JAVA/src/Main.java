import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    static class WordStat {
        String word;
        int count;

        WordStat(String word, int count) {
            this.word = word;
            this.count = count;
        }
    }

    static class FileStat {
        String fileName;
        int wordCount;

        FileStat(String fileName, int wordCount) {
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

    private static String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\uFEFF", "").toLowerCase(Locale.ROOT);
    }

    // Зведення подібних слів (єдиний регістр + прибирання розділових знаків і порожніх токенів)
    private static List<String> extractSimilarNormalizedWords(String text) {
        List<String> result = new ArrayList<>();
        String normalized = normalizeText(text);
        String[] tokens = normalized.split("[^\\p{L}\\p{N}]+");
        for (String token : tokens) {
            String w = token.trim();
            if (!w.isEmpty()) {
                result.add(w);
            }
        }
        return result;
    }

    // Зауваження 1 і 2: шлях з консолі; при відсутності файлу — повідомлення і повторний запит
    private static Path promptExistingInputFile(BufferedReader console, String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String line = console.readLine();
            if (line == null) {
                return null;
            }
            String cleaned = cleanPath(line);
            if (cleaned.isEmpty()) {
                System.out.println("Помилка: шлях не може бути порожнім. Спробуйте ще раз.");
                continue;
            }
            Path path = Paths.get(cleaned);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return path;
            }
            System.out.println("Помилка: вхідний файл не знайдено. Введіть шлях ще раз.");
        }
    }

    // Зауваження 3: при відсутності вихідного файлу — повідомлення і запит на створення
    private static Path promptOutputFile(BufferedReader console, String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String line = console.readLine();
            if (line == null) {
                return null;
            }
            String cleaned = cleanPath(line);
            if (cleaned.isEmpty()) {
                System.out.println("Помилка: шлях вихідного файлу не може бути порожнім.");
                continue;
            }
            Path path = Paths.get(cleaned);
            if (Files.exists(path)) {
                if (Files.isRegularFile(path)) {
                    return path;
                }
                System.out.println("Помилка: за вказаним шляхом папка, а не файл.");
                continue;
            }

            System.out.print("Вихідний файл відсутній. Створити його? (y/n): ");
            String answer = console.readLine();
            if (answer != null && isYes(answer)) {
                Path parent = path.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(path);
                System.out.println("Створено файл: " + path.toAbsolutePath());
                return path;
            } else {
                System.out.println("Файл не створено. Введіть інший шлях або спробуйте ще раз.");
            }
        }
    }

    // Зауваження 4: перезапис (o) або запис у кінець (a) існуючого файлу
    private static boolean promptAppendMode(BufferedReader console, Path outputFile) throws IOException {
        if (Files.exists(outputFile) && Files.size(outputFile) > 0) {
            while (true) {
                System.out.print("Файл уже існує і не порожній. Перезаписати (o) чи дописати в кінець (a)? [o/a]: ");
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
                System.out.println("Введіть 'o' для перезапису або 'a' для дописування.");
            }
        }
        return false;
    }

    private static boolean isYes(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        return v.equals("y") || v.equals("yes") || v.equals("так") || v.equals("т");
    }

    // Послідовне буферизоване читання файлу у рядок
    private static String readSequentiallyBuffered(Path filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    // Буферизований запис рядків у вихідний файл з підтримкою APPEND / TRUNCATE_EXISTING
    private static void writeLinesBuffered(Path outputFile, Collection<String> lines, boolean append) throws IOException {
        StandardOpenOption option = append
                ? StandardOpenOption.APPEND
                : StandardOpenOption.TRUNCATE_EXISTING;
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, option)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // ЗАВДАННЯ 1: пошук слів у тексті (зі зведенням подібних) + унікальні відсортовані слова у файл
    private static void runTask1SearchAndUniqueWords(BufferedReader console) throws IOException {
        System.out.println("\n--- Завдання 1: Пошук слів у тексті та унікальні слова (послідовний доступ) ---");
        Path wordsFile = promptExistingInputFile(console, "Введіть шлях до файлу зі словами для пошуку: ");
        if (wordsFile == null) {
            return;
        }
        Path textFile = promptExistingInputFile(console, "Введіть шлях до файлу з текстом: ");
        if (textFile == null) {
            return;
        }
        Path outFile = promptOutputFile(console, "Введіть шлях до вихідного файлу для унікальних слів: ");
        if (outFile == null) {
            return;
        }
        boolean append = promptAppendMode(console, outFile);

        List<String> targetList = extractSimilarNormalizedWords(readSequentiallyBuffered(wordsFile));
        List<String> textWords = extractSimilarNormalizedWords(readSequentiallyBuffered(textFile));

        Map<String, Integer> freqMap = new HashMap<>();
        Set<String> uniqueSortedTextWords = new TreeSet<>();

        for (String w : textWords) {
            freqMap.put(w, freqMap.getOrDefault(w, 0) + 1);
            uniqueSortedTextWords.add(w);
        }

        Set<String> uniqueTargets = new LinkedHashSet<>(targetList);
        List<WordStat> searchStats = new ArrayList<>();
        for (String target : uniqueTargets) {
            searchStats.add(new WordStat(target, freqMap.getOrDefault(target, 0)));
        }

        searchStats.sort((a, b) -> {
            if (b.count != a.count) {
                return Integer.compare(b.count, a.count);
            }
            return a.word.compareTo(b.word);
        });

        System.out.println("\nРезультати пошуку (за спаданням кількості входжень):");
        for (WordStat item : searchStats) {
            System.out.println(item.word + " -> " + item.count);
        }

        writeLinesBuffered(outFile, uniqueSortedTextWords, append);
        System.out.println("Унікальні відсортовані слова записано у: " + outFile.toAbsolutePath());
    }

    // ЗАВДАННЯ 2: підрахунок слів у кожному текстовому файлі (послідовний доступ)
    private static void runTask2CountWordsPerFile(BufferedReader console) throws IOException {
        System.out.println("\n--- Завдання 2: Підрахунок слів у кожному файлі (послідовний доступ) ---");
        List<FileStat> fileStats = new ArrayList<>();

        while (true) {
            Path filePath = promptExistingInputFile(console, "Введіть шлях до текстового файлу: ");
            if (filePath == null) {
                break;
            }

            int wordCount = 0;
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first) {
                        line = line.replace("\uFEFF", "");
                        first = false;
                    }
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        String[] parts = trimmed.split("\\s+");
                        wordCount += parts.length;
                    }
                }
            }

            String fileName = filePath.getFileName().toString();
            fileStats.add(new FileStat(fileName, wordCount));
            System.out.println("Файл: " + fileName + " | Кількість слів: " + wordCount);

            System.out.print("Ввести ще один файл? (y/n): ");
            String more = console.readLine();
            if (more == null || !isYes(more)) {
                break;
            }
        }

        System.out.print("Зберегти звіт підрахунку у вихідний файл? (y/n): ");
        String save = console.readLine();
        if (save != null && isYes(save)) {
            Path outFile = promptOutputFile(console, "Введіть шлях до вихідного файлу: ");
            if (outFile != null) {
                boolean append = promptAppendMode(console, outFile);
                List<String> reportLines = new ArrayList<>();
                for (FileStat st : fileStats) {
                    reportLines.add(st.fileName + " -> " + st.wordCount);
                }
                writeLinesBuffered(outFile, reportLines, append);
                System.out.println("Звіт збережено у: " + outFile.toAbsolutePath());
            }
        }
    }

    // ЗАВДАННЯ 3: прямий доступ до файлу через RandomAccessFile
    private static void runTask3RandomAccess(BufferedReader console) throws IOException {
        System.out.println("\n--- Завдання 3: Прямий доступ до файлу (RandomAccessFile) ---");
        Path filePath = promptExistingInputFile(console, "Введіть шлях до файлу для прямого доступу: ");
        if (filePath == null) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
            long length = raf.length();
            System.out.println("Розмір файлу (байт): " + length);

            long pos = 0;
            if (length > 0) {
                System.out.print("Введіть початкову позицію в байтах (0 .. " + (length - 1) + "): ");
                String posStr = console.readLine();
                if (posStr != null && !posStr.trim().isEmpty()) {
                    pos = Long.parseLong(posStr.trim());
                    if (pos < 0) pos = 0;
                    if (pos > length) pos = length;
                }
            }

            // Пряме позиціонування курсора у файлі
            raf.seek(pos);
            byte[] remainingBytes = new byte[(int) Math.max(0, length - pos)];
            raf.readFully(remainingBytes);

            String fragment = new String(remainingBytes, StandardCharsets.UTF_8);
            List<String> fragmentWords = extractSimilarNormalizedWords(fragment);

            System.out.println("Позиція читання: " + pos + " | Слів у фрагменті до кінця файлу: " + fragmentWords.size());

            System.out.print("Дописати контрольний рядок у кінець через прямий доступ (seek)? (y/n): ");
            String addNote = console.readLine();
            if (addNote != null && isYes(addNote)) {
                raf.seek(raf.length());
                String note = System.lineSeparator() + "[RandomAccessFile] Words from pos " + pos + ": " + fragmentWords.size();
                raf.write(note.getBytes(StandardCharsets.UTF_8));
                System.out.println("Запис виконано за позицією: " + raf.length());
            }
        }
    }

    public static void main(String[] args) {
        // Зауваження 5: буферизований консольний ввід-вивід
        try (BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            while (true) {
                System.out.println("\n==================================================");
                System.out.println("Оберіть завдання:");
                System.out.println("1 - Пошук слів у тексті + унікальні слова у файл (послідовний доступ)");
                System.out.println("2 - Підрахунок слів у кожному файлі (послідовний доступ)");
                System.out.println("3 - Робота з файлом через прямий доступ (RandomAccessFile)");
                System.out.println("4 - Виконати всі завдання послідовно (1 -> 2 -> 3)");
                System.out.println("0 - Вихід");
                System.out.print("Ваш вибір: ");

                String choice = console.readLine();
                if (choice == null) {
                    break;
                }

                switch (choice.trim()) {
                    case "1":
                        runTask1SearchAndUniqueWords(console);
                        break;
                    case "2":
                        runTask2CountWordsPerFile(console);
                        break;
                    case "3":
                        runTask3RandomAccess(console);
                        break;
                    case "4":
                        runTask1SearchAndUniqueWords(console);
                        runTask2CountWordsPerFile(console);
                        runTask3RandomAccess(console);
                        break;
                    case "0":
                        System.out.println("Завершення роботи.");
                        return;
                    default:
                        System.out.println("Невірний вибір. Введіть 0, 1, 2, 3 або 4.");
                }
            }
        } catch (IOException e) {
            System.out.println("Помилка вводу-виводу: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Помилка формату числа: " + e.getMessage());
        }
    }
}