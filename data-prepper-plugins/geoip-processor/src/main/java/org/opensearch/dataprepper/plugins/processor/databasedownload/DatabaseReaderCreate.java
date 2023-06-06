package org.opensearch.dataprepper.plugins.processor.databasedownload;

import com.maxmind.db.CHMCache;
import com.maxmind.db.NoCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import org.opensearch.dataprepper.plugins.processor.loadtype.LoadTypeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseReaderCreate {

    private static String[] DEFAULT_DATABASE_FILENAMES = new String[] { "GeoLite2-ASN.mmdb", "GeoLite2-City.mmdb", "GeoLite2-Country.mmdb" };
    public static DatabaseReader.Builder createLoader(Path databasePath, LoadTypeOptions loadDatabaseType, int cacheSize) {

        DatabaseReader.Builder builder = null;

        switch (loadDatabaseType) {
            case INMEMORY:
                builder = createDatabaseBuilder(databasePath).withCache(NoCache.getInstance());
                builder.fileMode(Reader.FileMode.MEMORY_MAPPED);
                break;
            case CACHE:
                builder = createDatabaseBuilder(databasePath).withCache(new CHMCache(cacheSize));
                break;
        }
        return builder;
    }

    public static DatabaseReader.Builder createDatabaseBuilder(Path databasePath) {
        return new DatabaseReader.Builder(databasePath.toFile());
    }
}
