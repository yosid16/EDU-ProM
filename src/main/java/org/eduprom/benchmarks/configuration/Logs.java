package org.eduprom.benchmarks.configuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Logs {

    public static final String CONTEST_2017 = "EventLogs\\contest_2017\\log%s.xes";
    public static final String CONTEST_2016 = "EventLogs\\contest_dataset\\training_log_%s.xes";

    private Set<String> files;

    public Logs(Set<String> files) {
        this.files = files;
    }

    public static class LogsBuilder {
        private Set<Integer> numbers;
        private Set<String> files;
        private Set<String> formats;

        private Set<String> getFormatFiles(String format){
            return this.numbers.stream().map(number -> String.format(format, number)).collect(Collectors.toSet());
        }

        public LogsBuilder() {
            files = new HashSet<>();
            numbers = new HashSet<>();
            formats = new HashSet<>();
        }

        public Set<Integer> getNumbers() {
            return numbers;
        }

        public LogsBuilder addNumbers(Integer... numbers) {
            this.numbers = Sets.newHashSet(numbers);
            return this;
        }

        public LogsBuilder addNumber(Integer number) {
            this.numbers.add(number);
            return this;
        }

        public LogsBuilder addNumbers(Integer min, Integer max) {
            for (int number = min; number <= max; number++) {
                addNumber(number);
            }

            return this;
        }

        public Set<String> getFiles() {
            return files;
        }

        public LogsBuilder addFile(String file) {
            this.files.add(file);
            return this;
        }

        public LogsBuilder addFormat(String format) {
            this.formats.add(format);
            return this;
        }

        public Logs build(){
             this.files.addAll(formats.stream()
                    .flatMap(format-> getFormatFiles(format).stream()).collect(Collectors.toList()));
             return new Logs(files);
        }
    }

    public Set<String> getFiles() {
        return files;
    }

    public static LogsBuilder getBuilder(){
        return new LogsBuilder();
    }
}
