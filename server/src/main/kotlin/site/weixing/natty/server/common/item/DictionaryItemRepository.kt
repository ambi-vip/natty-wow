//package site.weixing.natty.server.common.item
//
//import org.springframework.data.jpa.repository.JpaRepository
//import org.springframework.stereotype.Repository
//
///**
// * 字典项仓库接口
// */
//@Repository
//interface DictionaryItemRepository : JpaRepository<DictionaryItemEntity, String> {
//
//    /**
//     * 根据所属字典ID和字典项编码查询字典项实体
//     *
//     * @param dictionaryId 所属字典ID
//     * @param itemCode 字典项编码
//     * @return 字典项实体
//     */
//    fun findByDictionaryIdAndItemCode(dictionaryId: String, itemCode: String): DictionaryItemEntity?
//
//    /**
//     * 根据所属字典ID查询所有字典项实体
//     *
//     * @param dictionaryId 所属字典ID
//     * @return 字典项实体列表
//     */
//    fun findAllByDictionaryId(dictionaryId: String): List<DictionaryItemEntity>
//}