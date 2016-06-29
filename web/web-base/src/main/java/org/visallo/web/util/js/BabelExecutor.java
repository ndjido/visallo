package org.visallo.web.util.js;

import org.apache.commons.io.IOUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.script.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class BabelExecutor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(BabelExecutor.class);

    private ScriptEngine engine;
    private Bindings bindings;
    private Future babelFuture;
    private ExecutorService executorService;

    public BabelExecutor() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.babelFuture = this.executorService.submit(() -> initializeBabel());
    }

    public <T> Future<T> submit(Callable<T> task) {
        return this.executorService.submit(() -> {
            this.babelFuture.get();
            return task.call();
        });
    }

    public synchronized void compileWithSharedEngine(CachedCompilation cachedCompilation, SourceMapType sourceMapType) throws ScriptException {
        ScriptEngine engine = this.engine;
        Bindings bindings = this.bindings;
        bindings.put("input", cachedCompilation.getInput());
        bindings.put("source", cachedCompilation.getPath() + ".src");
        String transform = getTransformJavaScript();
        Object output = engine.eval(transform.replace("${SOURCEMAP}", sourceMapJsType(sourceMapType)), bindings);
        Bindings result = (Bindings) output;
        if (sourceMapType == SourceMapType.EXTERNAL) {
            String sourceMap = (String) result.get("sourceMap");
            cachedCompilation.setSourceMap(sourceMap);
        }
        cachedCompilation.setOutput((String) result.get("code"));
    }

    private String getTransformJavaScript() {
        String transform = null;
        try (StringWriter writer = new StringWriter()) {
            IOUtils.copy(getClass().getResourceAsStream("babel-transform.js"), writer, StandardCharsets.UTF_8);
            transform = writer.toString();
        } catch (IOException e) {
            throw new VisalloException("Unable to read babel transformer");
        }
        if (transform == null) {
            throw new VisalloException("Babel configuration not found");
        }
        return transform;
    }

    private String sourceMapJsType(SourceMapType sourceMapType) {
        switch (sourceMapType) {
            case EXTERNAL: return "true";
            case INLINE: return "\"inline\"";
            case NONE:
            default: return "false";
        }
    }

    private void initializeBabel() {
        try {
            long start = System.nanoTime();
            LOGGER.info("Initializing Babel Transformer...");
            InputStreamReader babelReader = new InputStreamReader(getClass().getResourceAsStream("babel.js"));

            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            SimpleBindings bindings = new SimpleBindings();

            engine.eval(babelReader, bindings);
            LOGGER.info("Babel Transformer initialized in %d seconds...", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
            this.engine = engine;
            this.bindings = bindings;
        } catch (Exception e) {
            throw new VisalloException("Unable to initialize babel");
        }
    }

}
