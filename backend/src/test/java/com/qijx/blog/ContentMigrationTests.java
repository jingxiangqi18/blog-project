package com.qijx.blog;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ContentMigrationTests {
    @Test
    void migrationPreservesHistoricalContentWithoutGuessingItsSection(){
        DriverManagerDataSource dataSource = database();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try{
            execute(dataSource, new ClassPathResource("legacy-schema.sql"));
            jdbc.update("""
                INSERT INTO users(id, username, password_hash, role, enabled, created_at, updated_at)
                VALUES(1, 'legacy-owner', 'old-hash', 'USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("INSERT INTO categories(id, name) VALUES(1, 'Mixed content')");
            jdbc.update("""
                INSERT INTO articles(id, title, content, category_id, author_id, created_at, updated_at)
                VALUES(1, 'Historical article', '[Basic](card:1)', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("""
                INSERT INTO article_drafts(article_id, author_id, title, content, category_id, created_at, updated_at)
                VALUES(1, 1, 'Unpublished draft', 'Draft body', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("""
                INSERT INTO article_revisions(article_id, revision_number, title, content, category_id, created_by, created_at)
                VALUES(1, 1, 'Historical article', 'Old revision', 1, 1, CURRENT_TIMESTAMP)
                """);
            jdbc.update("""
                INSERT INTO knowledge_cards(id, title, summary, content, created_by, created_at, updated_at)
                VALUES(1, 'Basic', 'Summary', 'Card body', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("""
                INSERT INTO comments(id, article_id, author_id, content, created_at, updated_at)
                VALUES(1, 1, 1, 'Comment', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("INSERT INTO article_likes VALUES(1, 1, CURRENT_TIMESTAMP)");
            jdbc.update("INSERT INTO article_favorites VALUES(1, 1, CURRENT_TIMESTAMP)");
            jdbc.update("INSERT INTO comment_likes VALUES(1, 1, CURRENT_TIMESTAMP)");

            execute(dataSource, new FileSystemResource("database/20261023_content_organization.sql"));
            execute(dataSource, new FileSystemResource("database/20261024_series_reading.sql"));

            assertThat(jdbc.queryForObject("SELECT content FROM articles WHERE id = 1", String.class)).isEqualTo("[Basic](card:1)");
            assertThat(jdbc.queryForObject("SELECT section_id FROM articles WHERE id = 1", Long.class)).isNull();
            assertThat(jdbc.queryForObject("SELECT section_id FROM categories WHERE id = 1", Long.class)).isNull();
            assertThat(jdbc.queryForObject("SELECT content_type FROM articles WHERE id = 1", String.class)).isEqualTo("general");
            assertThat(jdbc.queryForObject("SELECT summary FROM articles WHERE id = 1", String.class)).isNull();
            for(String table : List.of("users", "categories", "articles", "article_drafts", "article_revisions", "knowledge_cards",
                "comments", "article_likes", "article_favorites", "comment_likes")){
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class)).as(table).isEqualTo(1);
            }
            assertThat(jdbc.queryForList("SELECT slug FROM sections ORDER BY sort_order", String.class))
                .containsExactly("technology", "life");
        }finally{
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    void freshSchemaAndMigratedSchemaHaveMatchingColumnsIndexesAndForeignKeys() throws Exception{
        DriverManagerDataSource fresh = database();
        DriverManagerDataSource migrated = database();
        try{
            execute(fresh, new FileSystemResource("database/schema.sql"));
            execute(migrated, new ClassPathResource("legacy-schema.sql"));
            execute(migrated, new FileSystemResource("database/20261023_content_organization.sql"));
            execute(migrated, new FileSystemResource("database/20261024_series_reading.sql"));
            try(Connection freshConnection = fresh.getConnection(); Connection migratedConnection = migrated.getConnection()){
                assertThat(describe(freshConnection)).isEqualTo(describe(migratedConnection));
            }
        }finally{
            new JdbcTemplate(fresh).execute("SHUTDOWN");
            new JdbcTemplate(migrated).execute("SHUTDOWN");
        }
    }

    @Test
    void readingMigrationPreservesExistingSeriesMembershipAndOrder(){
        DriverManagerDataSource dataSource = database();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try{
            execute(dataSource, new ClassPathResource("content-organization-schema.sql"));
            jdbc.update("""
                INSERT INTO users(id, username, password_hash, role, enabled, created_at, updated_at)
                VALUES(1, 'owner', 'old-hash', 'USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("""
                INSERT INTO articles(id, title, content, author_id, created_at, updated_at)
                VALUES(1, 'Article', 'Historical content', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            jdbc.update("""
                INSERT INTO series(id, slug, title, section_id, created_by, created_at, updated_at)
                SELECT 1, 'java', 'Java', id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sections WHERE slug = 'technology'
                """);
            jdbc.update("INSERT INTO series_articles(series_id, article_id, sort_order) VALUES(1, 1, 7)");
            execute(dataSource, new FileSystemResource("database/20261024_series_reading.sql"));
            assertThat(jdbc.queryForObject("SELECT sort_order FROM series_articles WHERE series_id = 1 AND article_id = 1", Integer.class))
                .isEqualTo(7);
            assertThat(jdbc.queryForObject("SELECT chapter_id FROM series_articles WHERE series_id = 1 AND article_id = 1", Long.class))
                .isNull();
            assertThat(jdbc.queryForObject("SELECT content FROM articles WHERE id = 1", String.class)).isEqualTo("Historical content");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM series", Long.class)).isEqualTo(1);
        }finally{
            jdbc.execute("SHUTDOWN");
        }
    }

    private DriverManagerDataSource database(){
        return new DriverManagerDataSource("jdbc:h2:mem:content_" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
    }

    private void execute(DriverManagerDataSource dataSource, Resource resource){
        new ResourceDatabasePopulator(resource).execute(dataSource);
    }

    private Map<String, Object> describe(Connection connection) throws Exception{
        DatabaseMetaData metadata = connection.getMetaData();
        Map<String, Object> tables = new TreeMap<>();
        try(ResultSet tableRows = metadata.getTables(null, "public", null, new String[]{"TABLE"})){
            while(tableRows.next()){
                String table = tableRows.getString("TABLE_NAME");
                Map<String, Object> description = new TreeMap<>();
                Map<String, String> columns = new TreeMap<>();
                try(ResultSet rows = metadata.getColumns(null, "public", table, null)){
                    while(rows.next()){
                        columns.put(rows.getString("COLUMN_NAME"), rows.getString("TYPE_NAME") + ":" + rows.getInt("COLUMN_SIZE")
                            + ":" + rows.getInt("NULLABLE") + ":" + rows.getString("COLUMN_DEF"));
                    }
                }
                description.put("columns", columns);
                Map<String, List<String>> indexes = new HashMap<>();
                try(ResultSet rows = metadata.getIndexInfo(null, "public", table, false, false)){
                    while(rows.next()){
                        if(rows.getString("COLUMN_NAME") != null){
                            indexes.computeIfAbsent(rows.getString("INDEX_NAME"), key -> new ArrayList<>())
                                .add(rows.getBoolean("NON_UNIQUE") + ":" + rows.getString("COLUMN_NAME"));
                        }
                    }
                }
                var indexShapes = new TreeSet<String>();
                indexes.values().forEach(values -> indexShapes.add(String.join(",", values)));
                description.put("indexes", indexShapes);
                var foreignKeys = new TreeSet<String>();
                try(ResultSet rows = metadata.getImportedKeys(null, "public", table)){
                    while(rows.next()){
                        foreignKeys.add(rows.getString("FKCOLUMN_NAME") + ":" + rows.getString("PKTABLE_NAME")
                            + ":" + rows.getString("PKCOLUMN_NAME") + ":" + rows.getInt("DELETE_RULE"));
                    }
                }
                description.put("foreignKeys", foreignKeys);
                tables.put(table, description);
            }
        }
        return tables;
    }
}
