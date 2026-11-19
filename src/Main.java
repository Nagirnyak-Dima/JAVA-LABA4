import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

 class Task1WordCounterRobust {


    public static File getValidInputFile(BufferedReader consoleReader, String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String pathStr = consoleReader.readLine();
            if (pathStr == null) {
                System.exit(0);
            }
            File file = new File(pathStr.trim());
            if (file.exists() && file.isFile()) {
                return file;
            }
            System.out.println("Помилка: Файл не знайдено або це недійсний шлях. Будь ласка, спробуйте ще раз.");
        }
    }

    // Допоміжний метод для отримання вихідного файлу з можливістю створення 
    public static File getValidOutputFile(BufferedReader consoleReader, String prompt) throws IOException {
        while (true) {
            System.out.print(prompt);
            String pathStr = consoleReader.readLine();
            if (pathStr == null) {
                System.exit(0);
            }
            File file = new File(pathStr.trim());
            if (!file.exists()) {
                System.out.print("Файл не існує. Бажаєте створити його? (y/n): ");
                String choice = consoleReader.readLine();
                if (choice != null && (choice.trim().equalsIgnoreCase("y") || choice.trim().equalsIgnoreCase("так"))) {
                    try {
                        if (file.createNewFile()) {
                            return file;
                        }
                    } catch (IOException e) {
                        System.out.println("Не вдалося створити файл: " + e.getMessage());
                    }
                }
            } else {
                return file;
            }
        }
    }

    public static void main(String[] args) {
        // Буферизований консольний ввід-вивід 
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("=== ЗАВДАННЯ 1: Підрахунок слів та унікальні слова ===");

            // 1. Отримання вхідних файлів
            File wordsFile = getValidInputFile(consoleReader, "Введіть шлях до файлу зі списком шуканих слів: ");
            File textFile = getValidInputFile(consoleReader, "Введіть шлях до файлу з текстом для пошуку: ");
            File outputFile = getValidOutputFile(consoleReader, "Введіть шлях до вихідного файлу для результату: ");

            // 4. Вибір режиму запису: перезапис чи дописування (Зауваження 4)
            boolean appendMode = false;
            if (outputFile.exists() && outputFile.length() > 0) {
                System.out.print("Файл вже містить дані. Бажаєте записати в кінець файлу (append) чи перезаписати (overwrite)? [a/o]: ");
                String modeChoice = consoleReader.readLine();
                if (modeChoice != null && modeChoice.trim().equalsIgnoreCase("a")) {
                    appendMode = true;
                }
            }

            // Зчитування слів для пошуку через буферизований потік
            List<String> searchWords = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(wordsFile.toPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    searchWords.add(line);
                }
            }

            // Зчитування тексту для пошуку
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader reader = Files.newBufferedReader(textFile.toPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
            }
            String content = contentBuilder.toString();

            // Обробка слів
            String[] allTextWords = content.toLowerCase().split("\\W+");
            Map<String, Integer> frequencyMap = new HashMap<>();
            for (String word : allTextWords) {
                if (!word.isEmpty()) {
                    frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
                }
            }

            Map<String, Integer> searchResults = new LinkedHashMap<>();
            for (String rawWord : searchWords) {
                String word = rawWord.trim().toLowerCase();
                if (!word.isEmpty()) {
                    int count = frequencyMap.getOrDefault(word, 0);
                    searchResults.put(word, count);
                }
            }

            // Сортування результатів за спаданням кількості входжень
            List<Map.Entry<String, Integer>> sortedSearchResults = new ArrayList<>(searchResults.entrySet());
            sortedSearchResults.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            System.out.println("\n--- Результати пошуку (сортування за спаданням) ---");
            for (Map.Entry<String, Integer> entry : sortedSearchResults) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }

            // Формування унікальних слів з тексту
            Set<String> uniqueWords = new TreeSet<>();
            for (String word : allTextWords) {
                if (!word.isEmpty()) {
                    uniqueWords.add(word);
                }
            }

            // Запис результатів у вихідний файл з використанням буферизації та обраного режиму (append/overwrite)
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8,
                    appendMode ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING)) {
                for (String word : uniqueWords) {
                    writer.write(word);
                    writer.newLine();
                }
            }

            System.out.println("\nУспішно! Унікальні слова записано у файл: " + outputFile.getPath());

        } catch (IOException e) {
            System.err.println("Помилка введення-виведення: " + e.getMessage());
        }
    }
}
