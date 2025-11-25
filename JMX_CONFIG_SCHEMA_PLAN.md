## Config-Driven Form Plan (Based on JMeter API)

The table below captures the high-priority JMeter `TestElement` types we need to support through a schema-driven editor. Each entry lists the canonical Java class, key properties exposed in the GUI, and special considerations when mapping between JSON and `.jmx` files.

| Element | JMeter Class | Primary GUI Fields | Notes |
| --- | --- | --- | --- |
| Test Plan | `org.apache.jmeter.testelement.TestPlan` | `name`, `functionalMode`, `serializeThreadGroups`, `tearDownOnShutdown`, user variables (`Arguments`) | GUI: `TestPlanGui`. User variables map to a named parameter table. |
| Thread Group | `org.apache.jmeter.threads.ThreadGroup` | `numThreads`, `rampUp`, `scheduler`, `duration`, `delay`, `loops` (`LoopController`), `continueForever` | Loop controller lives inside the thread group; expose it as nested fields. |
| Open Model Thread Group | `org.apache.jmeter.threads.openmodel.OpenModelThreadGroup` | `loadProfile` segments (arrival rate, duration, unit) | Requires `OpenModelThreadGroupGui`. |
| Stepping Thread Group | `kg.apc.jmeter.threads.SteppingThreadGroup` | `startUsers`, `usersPerStep`, `rampUp`, `hold`, `steps`, `flightTime` | Comes from JMeter Plugins. |
| HTTP Request | `org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy` | `protocol`, `domain`, `port`, `path`, `method`, `contentEncoding`, `arguments`, `bodyData`, flags (`followRedirects`, `autoRedirects`, `useKeepAlive`), timeouts, implementation, proxy info | Same schema covers both `HTTPSampler` and `HTTPSamplerProxy`. |
| HTTP Header Manager | `org.apache.jmeter.protocol.http.control.HeaderManager` | Repeated `name` / `value` pairs | Represented as a `CollectionProperty`. |
| Cookie / Cache / Auth Managers | `CookieManager`, `CacheManager`, `AuthManager` | `clearEachIteration`, cookie list, cache TTL, credential entries | All under `org.apache.jmeter.protocol.http.control`. |
| Arguments | `org.apache.jmeter.config.Arguments` | Repeated `name`, `value`, `metadata` rows | Used for user variables and sampler parameters. |
| CSV Data Set Config | `org.apache.jmeter.config.CSVDataSet` | `filename`, `fileEncoding`, `variableNames`, `delimiter`, `ignoreFirstLine`, `recycle`, `stopThread`, `shareMode` | GUI: `TestBeanGUI`. |
| Java Sampler | `org.apache.jmeter.protocol.java.sampler.JavaSampler` | `classname`, parameter table (`name`, `value`, `description`) | GUI: `JavaTestSamplerGui`. |
| JSR223 Sampler / Pre/Post | `org.apache.jmeter.protocol.java.sampler.JSR223Sampler` and related processors | `script`, `language`, `cacheKey`, `parameters`, `fileName` | Backed by `org.apache.jmeter.util.JSR223BeanInfoSupport`. |
| BeanShell Sampler / Pre/Post | `org.apache.jmeter.protocol.java.sampler.BeanShellSampler` | `script`, `fileName`, `parameters`, `resetInterpreter` | Legacy but widely used. |
| Controllers | `IfController`, `WhileController`, `SwitchController`, `ForEachController`, `IncludeController`, `RuntimeController`, `TransactionController`, `CriticalSectionController` | Conditional expressions, loop variables, include file path, runtime seconds, parent sample flags | Located in `org.apache.jmeter.control`. |
| Assertions | `ResponseAssertion`, `DurationAssertion`, `SizeAssertion`, `XPathAssertion`, `JSONPathAssertion` | Pattern list, test type, scope, expected values | Classes live in `org.apache.jmeter.assertions`. |
| Timers | `ConstantTimer`, `GaussianRandomTimer`, `UniformRandomTimer`, `ConstantThroughputTimer`, `PreciseThroughputTimer` | Delay, deviation, throughput targets, calculation mode | Under `org.apache.jmeter.timers`. |
| Processors | `UserParameters`, `RegExUserParameters`, `BeanShellPreProcessor`, `JSR223PostProcessor`, `XPathExtractor`, `JSONPostProcessor`, `BoundaryExtractor` | Parameter tables plus script / path options | Provide reusable widgets for structured inputs. |
| Sampler Extras | `SMTPSampler`, `JDBCSampler`, `TCPSampler`, `LDAPSampler`, `JMSSampler`, `MQTTSampler` | Protocol-specific connection fields | Each maps directly to its `TestElement` setters. |
| Config Elements | `ConfigTestElement`, `HTTPDefaults`, `JDBCConnectionConfiguration`, `KeystoreConfig`, `FTPSamplerConfig` | Default values for child samplers | Use their `TestBean` descriptors. |
| Result Collectors / Listeners | `org.apache.jmeter.reporters.ResultCollector` plus visualizers | `filename`, `logErrors`, `sampleSaveConfiguration` flags | `SampleSaveConfiguration` controls column persistence. |
| Backend Listener | `org.apache.jmeter.visualizers.backend.BackendListener` | `classname`, arguments (`Arguments`) | Commonly points to InfluxDB/Telegraf backends. |

### Key Practices

1. **Schema Registry** – Maintain a JSON/JS object that maps each supported element type to:
   - `testClass` and `guiClass`
   - allowed parent types (`ThreadGroup`, `TestPlan`, etc.)
   - field descriptors (`type`, `label`, `default`, `component`)
   - optional complex widgets (parameter tables, key-value lists)

2. **Native Property Keys** – Persist field values using the exact property names that the JMeter API expects (e.g., `HTTPSampler.domain`, `ThreadGroup.num_threads`). This allows direct serialization/deserialization with minimal mapping.

3. **Preserve Unknown Fields** – Always keep the entire property map from the backend. Schema-driven forms only edit the fields they know about; everything else is merged back untouched to guarantee lossless round-tripping.

4. **Extensible Widgets** – Build reusable Vue components for common field types (text, number, switch, select, key-value list, table). New elements can reuse these widgets by only adding schema metadata—no new Vue files needed.

5. **Backend Reflection (Future Work)** – For advanced integration, the backend can consume the same schema to dynamically instantiate `TestElement` classes via reflection, minimizing hard-coded switch statements in `JmxParserServiceImpl`.


