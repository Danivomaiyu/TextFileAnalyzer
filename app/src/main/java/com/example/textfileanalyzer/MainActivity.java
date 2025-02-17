package com.example.textfileanalyzer;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    Button selectFile,savePdf;
    private static final int REQUEST_CODE = 200;
    private String loadedContent = "";
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectFile = findViewById(R.id.selectBtn);
        textView = findViewById(R.id.displayText);
        savePdf = findViewById(R.id.savePdfBtn);

        savePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadedContent.isEmpty()) {
                    Log.e("FAile","Failed");
                    //Toast.makeText(this,"",Toast.LENGTH_LONG).show();
                } else {
                    String stats = generateStats(loadedContent);
                    String randomParagraph = generateRandomParagraph(getWordFrequency(loadedContent),50,1.0);
                    saveStatisticsToPdf(stats,randomParagraph);
                }
            }
        });

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });


    }

    private void saveStatisticsToPdf(String stats, String randomParagraph) {
        String contentToSave = stats + "\n\nRandom Paragraph:\n" + randomParagraph;
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595,842,1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        int x = 20;
        int y = 40;
        String[] lines = contentToSave.split("\n");
        for (String line : lines) {
            canvas.drawText(line, x, y, paint);
            y += 20; // Move to the next line
        }
        pdfDocument.finishPage(page);
        File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "statistics.pdf");
        Log.e("FileSAVED", pdfFile.toString());
        try {
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(this, "PDF saved to: " + pdfFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }


    private String generateRandomParagraph(Map<String, Integer> wordFreq, int wordCount, double temperature) {
        List<String> words = new ArrayList<>(wordFreq.keySet());
        List<Integer> frequencies = new ArrayList<>(wordFreq.values());
        double totalFrequency = frequencies.stream().mapToInt(Integer::intValue).sum();
        List<Double> probabilities = frequencies.stream()
                .map(freq -> Math.pow(freq / totalFrequency, 1 / temperature))
                .toList();
        double probabilitySum = probabilities.stream().mapToDouble(Double::doubleValue).sum();
        probabilities = probabilities.stream()
                .map(prob -> prob / probabilitySum)
                .toList();
        StringBuilder paragraph = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            double rand = Math.random();
            double cumulativeProbability = 0.0;
            for (int j = 0; j < probabilities.size(); j++) {
                cumulativeProbability += probabilities.get(j);
                if (rand < cumulativeProbability) {
                    paragraph.append(words.get(j)).append(" ");
                    break;
                }
            }
        }
        return paragraph.toString().trim();
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent,REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String mimeType = getContentResolver().getType(uri);
                if ("text/plain".equals(mimeType)) {
                    loadTextFile(uri);
                }else if("application/pdf".equals(mimeType)) {
                    loadPdfFile(uri);
                }else {
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadPdfFile(Uri uri) {
        //StringBuilder pdfContent = new StringBuilder();
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            loadedContent =contentBuilder.toString();
            displayStatistics(loadedContent);
        }catch (IOException e) {
            Toast.makeText(this,"Failed to load pdf file",Toast.LENGTH_LONG).show();
        }

    }

    private void loadTextFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            loadedContent =contentBuilder.toString();
            displayStatistics(loadedContent);
        } catch (IOException e) {
            Toast.makeText(this, "Error loading the file", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayStatistics(String loadedContent) {
        String stats = generateStats(loadedContent);
        textView.setText(stats);

    }

    private String generateStats(String loadedContent) {
        int wordCount = getWordCount(loadedContent);
        int sentenceCount = getSentenceCount(loadedContent);
        Map<String, Integer> wordFreq = getWordFrequency(loadedContent);
        List<String> uniqueWords = getUniqueWords(wordFreq);
        List<Map.Entry<String, Integer>> topWords = getTopFrequentWords(wordFreq);
        StringBuilder stats = new StringBuilder();
        stats.append("Word Count: ").append(wordCount).append("\n");
        stats.append("Sentence Count: ").append(sentenceCount).append("\n");
        stats.append("Unique Words: ").append(uniqueWords.size()).append("\n");
        stats.append("Top 5 Words:\n");
        for (Map.Entry<String, Integer> entry : topWords) {
            stats.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return stats.toString();
    }

    private List<Map.Entry<String, Integer>> getTopFrequentWords(Map<String, Integer> wordFreq) {
        return wordFreq.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .toList();
    }


    private List<String> getUniqueWords(Map<String, Integer> wordFreq) {
        return new ArrayList<>(wordFreq.keySet());
    }

    private Map<String, Integer> getWordFrequency(String loadedContent) {
        Map<String, Integer> wordFreq = new HashMap<>();
        String[] words = loadedContent.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");

        for (String word : words) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }

        return wordFreq;
    }

    private int getSentenceCount(String loadedContent) {
        return loadedContent.split("[.!?]").length;
    }

    private int getWordCount(String loadedContent) {
        return loadedContent.split("\\s+").length;
    }
}