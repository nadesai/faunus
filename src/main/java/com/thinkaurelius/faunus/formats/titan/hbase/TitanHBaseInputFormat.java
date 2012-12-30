package com.thinkaurelius.faunus.formats.titan.hbase;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.thinkaurelius.titan.diskstorage.Backend;
import com.thinkaurelius.titan.diskstorage.hbase.HBaseStoreManager;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableRecordReader;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.*;

import java.io.IOException;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TitanHBaseInputFormat extends InputFormat implements Configurable {

    private static final String HOSTNAME_KEY = HBaseStoreManager.HBASE_CONFIGURATION_MAP.get(GraphDatabaseConfiguration.HOSTNAME_KEY);
    private static final String PORT_KEY = HBaseStoreManager.HBASE_CONFIGURATION_MAP.get(GraphDatabaseConfiguration.PORT_KEY);
    static final byte[] EDGE_STORE_FAMILY = Bytes.toBytes(Backend.EDGESTORE_NAME);

    private final TableInputFormat tableInputFormat;
    private FaunusTitanHBaseGraph graph;
    private boolean pathEnabled;


    public TitanHBaseInputFormat() {
        this.tableInputFormat = new TableInputFormat();
    }

    @Override
    public List<InputSplit> getSplits(final JobContext jobContext) throws IOException, InterruptedException {
        return this.tableInputFormat.getSplits(jobContext);
    }

    @Override
    public RecordReader<NullWritable, FaunusVertex> createRecordReader(final InputSplit inputSplit, final TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        return new TitanHBaseRecordReader(this.graph, this.pathEnabled, (TableRecordReader) this.tableInputFormat.createRecordReader(inputSplit, taskAttemptContext));
    }

    @Override
    public void setConf(final Configuration config) {

        config.set(TableInputFormat.SCAN_COLUMN_FAMILY, Backend.EDGESTORE_NAME);
        this.tableInputFormat.setConf(config);

        final BaseConfiguration titanconfig = new BaseConfiguration();
        //General Titan configuration for read-only
        titanconfig.setProperty("storage.read-only", "true");
        titanconfig.setProperty("autotype", "none");
        // HBase specific configuration
        titanconfig.setProperty("storage.backend", "hbase");
        titanconfig.setProperty("storage.tablename", config.get(TableInputFormat.INPUT_TABLE));
        titanconfig.setProperty("storage.hostname", config.get(HOSTNAME_KEY));
        if (config.get(PORT_KEY, null) != null)
            titanconfig.setProperty("storage.port", config.get(PORT_KEY));
        this.graph = new FaunusTitanHBaseGraph(titanconfig);
        this.pathEnabled = config.getBoolean(FaunusCompiler.PATH_ENABLED, false);
    }

    @Override
    public Configuration getConf() {
        return tableInputFormat.getConf();
    }


}