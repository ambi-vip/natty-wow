//package site.weixing.natty.server.dictionary.dictionary
//
//import org.springframework.data.jpa.repository.JpaRepository
//import org.springframework.stereotype.Repository
//
///**
// * 字典仓库接口
// */
//@Repository
//interface DictionaryRepository : JpaRepository<DictionaryEntity, String> {
//
//    /**
//     * 根据字典编码查询字典实体
//     *
//     * @param code 字典编码
//     * @return 字典实体
//     */
//    fun findByCode(code: String): DictionaryEntity?
//}