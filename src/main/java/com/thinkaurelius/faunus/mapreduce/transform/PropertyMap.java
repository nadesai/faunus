package com.thinkaurelius.faunus.mapreduce.transform;

import com.thinkaurelius.faunus.FaunusEdge;
import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.Tokens;
import com.thinkaurelius.faunus.mapreduce.util.ElementPicker;
import com.thinkaurelius.faunus.mapreduce.util.WritableHandler;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PropertyMap {

    public static final String CLASS = Tokens.makeNamespace(PropertyMap.class) + ".class";
    public static final String KEY = Tokens.makeNamespace(PropertyMap.class) + ".key";
    public static final String TYPE = Tokens.makeNamespace(PropertyMap.class) + ".type";

    public enum Counters {
        VERTICES_PROCESSED,
        OUT_EDGES_PROCESSED
    }

    public static class Map extends Mapper<NullWritable, FaunusVertex, WritableComparable, WritableComparable> {

        private String key;
        private boolean isVertex;
        private WritableHandler handler;

        @Override
        public void setup(final Mapper.Context context) throws IOException, InterruptedException {
            this.isVertex = context.getConfiguration().getClass(CLASS, Element.class, Element.class).equals(Vertex.class);
            this.key = context.getConfiguration().get(KEY);
            this.handler = new WritableHandler(context.getConfiguration().getClass(TYPE, Text.class, WritableComparable.class));
        }

        private LongWritable longWritable = new LongWritable();

        @Override
        public void map(final NullWritable key, final FaunusVertex value, final Mapper<NullWritable, FaunusVertex, WritableComparable, WritableComparable>.Context context) throws IOException, InterruptedException {
            if (this.isVertex) {
                if (value.hasPaths()) {
                    this.longWritable.set(value.getIdAsLong());
                    WritableComparable writable = this.handler.set(ElementPicker.getProperty(value, this.key));
                    for (int i = 0; i < value.pathCount(); i++) {
                        context.write(this.longWritable, writable);
                    }
                    context.getCounter(Counters.VERTICES_PROCESSED).increment(1l);
                }
            } else {
                long edgesProcessed = 0;
                for (final Edge e : value.getEdges(Direction.OUT)) {
                    final FaunusEdge edge = (FaunusEdge) e;
                    if (edge.hasPaths()) {
                        this.longWritable.set(edge.getIdAsLong());
                        WritableComparable writable = this.handler.set(ElementPicker.getProperty(edge, this.key));
                        for (int i = 0; i < edge.pathCount(); i++) {
                            context.write(this.longWritable, writable);
                        }
                        edgesProcessed++;
                    }
                }
                context.getCounter(Counters.OUT_EDGES_PROCESSED).increment(edgesProcessed);
            }
        }
    }
}
