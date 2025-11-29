export const schemaElements = {
  DubboSampler: {
    label: 'Dubbo Sampler',
    category: 'sampler',
    testClass: 'com.alibaba.jmeter.plugin.dubbo.sample.DubboSampler',
    guiClass: 'com.alibaba.jmeter.plugin.dubbo.gui.DubboSamplerGui',
    allowedParents: ['ThreadGroup', 'kg.apc.jmeter.threads.SteppingThreadGroup'],
    defaults: {
      'dubbo.application': '',
      'dubbo.registryProtocol': 'zookeeper',
      'dubbo.registryAddress': '127.0.0.1:2181',
      'dubbo.interface': '',
      'dubbo.method': '',
      'dubbo.version': '',
      'dubbo.timeout': 3000,
      'dubbo.retries': 2,
      'dubbo.loadbalance': 'random',
      'dubbo.parameters': []
    },
    fields: [
      {
        key: 'dubbo.application',
        label: 'Application Name',
        type: 'string',
        placeholder: 'demo-consumer'
      },
      {
        key: 'dubbo.registryProtocol',
        label: 'Registry Protocol',
        type: 'select',
        options: [
          { label: 'zookeeper', value: 'zookeeper' },
          { label: 'nacos', value: 'nacos' },
          { label: 'multicast', value: 'multicast' },
          { label: 'redis', value: 'redis' },
          { label: 'p2p', value: 'p2p' }
        ],
        default: 'zookeeper'
      },
      {
        key: 'dubbo.registryAddress',
        label: 'Registry Address',
        type: 'string',
        placeholder: '127.0.0.1:2181'
      },
      {
        key: 'dubbo.interface',
        label: 'Interface',
        type: 'string',
        placeholder: 'com.demo.DemoService'
      },
      {
        key: 'dubbo.method',
        label: 'Method',
        type: 'string',
        placeholder: 'sayHello'
      },
      {
        key: 'dubbo.version',
        label: 'Version',
        type: 'string',
        placeholder: '1.0.0'
      },
      {
        key: 'dubbo.timeout',
        label: 'Timeout (ms)',
        type: 'number',
        min: 0,
        step: 100,
        default: 3000
      },
      {
        key: 'dubbo.retries',
        label: 'Retries',
        type: 'number',
        min: 0,
        max: 10,
        default: 2
      },
      {
        key: 'dubbo.loadbalance',
        label: 'Load Balance',
        type: 'select',
        options: [
          { label: 'Random', value: 'random' },
          { label: 'Round Robin', value: 'roundrobin' },
          { label: 'Least Active', value: 'leastactive' }
        ],
        default: 'random'
      },
      {
        key: 'dubbo.parameters',
        label: 'Invocation Parameters',
        type: 'kv-list',
        keyLabel: 'Param Name',
        valueLabel: 'Value',
        addLabel: 'Add Parameter'
      }
    ]
  }
}

/**
 * Returns schema metadata for provided element type.
 * Also matches by testClass for plugin types.
 */
export const getSchemaForType = (type) => {
  // Direct match
  if (schemaElements[type]) {
    return schemaElements[type]
  }
  
  // Match by testClass for plugin types
  // e.g., 'io.github.ningyu.jmeter.plugin.dubbo.sample.DubboSample' -> 'DubboSampler'
  if (type && type.includes('dubbo') && type.includes('DubboSample')) {
    return schemaElements['DubboSampler']
  }
  
  return null
}

/**
 * Returns all schema definitions as an array for iteration.
 */
export const getAllSchemas = () => Object.values(schemaElements)


