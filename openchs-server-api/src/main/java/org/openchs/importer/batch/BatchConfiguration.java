package org.openchs.importer.batch;

import org.openchs.importer.batch.model.Row;
import org.openchs.service.S3Service;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FlatFileFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobRepository jobRepository;
    private final S3Service s3Service;

    @Autowired
    public BatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, JobRepository jobRepository, S3Service s3Service) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobRepository = jobRepository;
        this.s3Service = s3Service;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Row> csvFileItemReader(@Value("#{jobParameters['s3Key']}") String s3Key) throws IOException {
        BufferedReader csvReader = new BufferedReader(new InputStreamReader(s3Service.getObjectContent(s3Key)));
        final String[] headers = csvReader.readLine().split(",");
        csvReader.close();

        DefaultLineMapper<Row> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(new DelimitedLineTokenizer());
        lineMapper.setFieldSetMapper(fieldSet -> new Row(headers, fieldSet.getValues()));

        return new FlatFileItemReaderBuilder<Row>()
                .name("locationReader")
                .resource(new InputStreamResource(s3Service.getObjectContent(s3Key)))
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    @Bean
    public Job importJob(ErrorFileCreatorListener listener, Step importStep) {
        return jobBuilderFactory.get("importJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(importStep)
                .end()
                .build();
    }

    @Bean
    public Step importStep(FlatFileItemReader<Row> csvFileItemReader,
                           CsvFileItemWriter csvFileItemWriter,
                           ErrorFileWriterListener errorFileWriterListener) {
        return stepBuilderFactory.get("importStep")
                .<Row, Row>chunk(1)
                .reader(csvFileItemReader)
                .writer(csvFileItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .noSkip(FileNotFoundException.class)
                .noSkip(FlatFileParseException.class)
                .noSkip(FlatFileFormatException.class)
                .skipPolicy((error, count) -> true)
                .listener(errorFileWriterListener)
                .build();
    }

    @Bean
    public JobLauncher bgJobLauncher() {
        return new SimpleJobLauncher() {{
            setJobRepository(jobRepository);
            setTaskExecutor(new ThreadPoolTaskExecutor() {{
                setCorePoolSize(1);
                setMaxPoolSize(1);
                setQueueCapacity(100);
                initialize();
            }});
        }};
    }

}