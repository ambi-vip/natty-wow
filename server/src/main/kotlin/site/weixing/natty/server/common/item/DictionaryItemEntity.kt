// package site.weixing.natty.server.common.item
//
// import jakarta.persistence.Column
// import jakarta.persistence.Entity
// import jakarta.persistence.EnumType
// import jakarta.persistence.Enumerated
// import jakarta.persistence.Id
// import jakarta.persistence.Index
// import jakarta.persistence.Table
// import site.weixing.natty.domain.common.item.DictionaryItemState.DictionaryItemStatus
//
// /**
// * 字典项查询实体
// *
// * @param id 字典项ID
// * @param dictionaryId 所属字典的ID
// * @param itemCode 字典项编码
// * @param itemName 字典项名称
// * @param itemValue 字典项值
// * @param sortOrder 排序
// * @param description 字典项描述
// * @param status 字典项状态
// * @param localizedNames 多语言名称
// */
// @Entity
// @Table(
//    name = "dictionary_item_read_model", indexes = [
//        Index(name = "idx_dictionary_item_dictionary_id_item_code", columnList = "dictionary_id,item_code", unique = true)
//    ]
// )
// data class DictionaryItemEntity(
//    @Id
//    val id: String,
//
//    @Column(nullable = false)
//    val dictionaryId: String,
//
//    @Column(nullable = false)
//    var itemCode: String,
//
//    @Column(nullable = false)
//    var itemName: String,
//
//    @Column(nullable = false)
//    var itemValue: String,
//
//    @Column(nullable = false)
//    var sortOrder: Int,
//
//    var description: String? = null,
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    var status: DictionaryItemStatus,
//
//    @Column(columnDefinition = "TEXT")
//    var localizedNames: String? = null // 存储JSON字符串
// )
