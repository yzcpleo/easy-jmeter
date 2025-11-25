import _axios, { post, get, put } from '@/lin/plugin/axios'

class Jmx {
  // ==================== JMX Structure Management ====================
  
  /**
   * Parse JMX file to structure
   * @param {Number} caseId - Case ID
   * @returns {Promise}
   */
  async parseJmx(caseId) {
    return _axios({
      method: 'get',
      url: `/v1/case/${caseId}/jmx/parse`,
    })
  }
  
  /**
   * Get JMX tree structure
   * @param {Number} caseId - Case ID
   * @returns {Promise}
   */
  async getJmxTree(caseId) {
    return _axios({
      method: 'get',
      url: `/v1/case/${caseId}/jmx/tree`,
    })
  }
  
  /**
   * Save JMX structure
   * @param {Number} caseId - Case ID
   * @param {Object} structure - JMX tree structure (JmxTreeNodeDTO)
   * @returns {Promise}
   */
  async saveJmxStructure(caseId, structure) {
    // Backend expects JmxStructureDTO with caseId and structure fields
    console.log('API saveJmxStructure called with caseId:', caseId, 'structure:', structure)
    
    // Ensure caseId is a valid number (not null/undefined)
    const validCaseId = caseId != null ? parseInt(caseId) : null
    if (!validCaseId) {
      throw new Error('Case ID is required for saving JMX structure')
    }
    
    return _axios({
      method: 'post',
      url: `/v1/case/${validCaseId}/jmx/structure`,
      data: {
        caseId: validCaseId,  // Include caseId in DTO
        structure: structure,  // This should be the root JmxTreeNodeDTO
      },
    })
  }
  
  /**
   * Generate JMX file from structure
   * @param {Number} caseId - Case ID
   * @returns {Promise}
   */
  async generateJmx(caseId) {
    return _axios({
      method: 'post',
      url: `/v1/case/${caseId}/jmx/generate`,
    })
  }
  
  /**
   * Get all versions
   * @param {Number} caseId - Case ID
   * @returns {Promise}
   */
  async getJmxVersions(caseId) {
    return _axios({
      method: 'get',
      url: `/v1/case/${caseId}/jmx/versions`,
    })
  }
  
  /**
   * Get JMX structure by version
   * @param {Number} caseId - Case ID
   * @param {Number} version - Version number
   * @returns {Promise}
   */
  async getJmxByVersion(caseId, version) {
    return _axios({
      method: 'get',
      url: `/v1/case/${caseId}/jmx/version/${version}`,
    })
  }
  
  // ==================== JMX Builder ====================
  
  /**
   * Create empty test plan
   * @param {String} name - Test plan name
   * @returns {Promise}
   */
  async createEmptyTestPlan(name = 'Test Plan') {
    return _axios({
      method: 'post',
      url: '/v1/jmx/builder/create',
      params: { name },
    })
  }
  
  /**
   * Get template
   * @param {String} templateName - Template name (simple, load_test, stress_test)
   * @returns {Promise}
   */
  async getTemplate(templateName) {
    return _axios({
      method: 'post',
      url: `/v1/jmx/builder/template/${templateName}`,
    })
  }
  
  /**
   * Validate JMX structure
   * @param {Number} caseId - Case ID
   * @param {Object} structure - JMX tree structure (JmxTreeNodeDTO)
   * @returns {Promise}
   */
  async validateStructure(caseId, structure) {
    // Backend expects JmxStructureDTO with caseId and structure fields
    console.log('API validateStructure called with caseId:', caseId, 'structure:', structure)
    
    // Ensure caseId is a valid number (not null/undefined)
    const validCaseId = caseId != null ? parseInt(caseId) : 0
    
    return _axios({
      method: 'post',
      url: '/v1/jmx/builder/validate',
      data: {
        caseId: validCaseId,  // Ensure caseId is a number
        structure: structure,  // This should be the root JmxTreeNodeDTO
      },
    })
  }
  
  /**
   * List available templates
   * @returns {Promise}
   */
  async listTemplates() {
    return _axios({
      method: 'get',
      url: '/v1/jmx/builder/templates',
    })
  }
}

export default new Jmx()

