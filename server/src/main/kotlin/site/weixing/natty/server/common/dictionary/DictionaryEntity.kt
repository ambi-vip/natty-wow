// package site.weixing.natty.server.dictionary.dictionary
//
// import jakarta.persistence.Column
// import jakarta.persistence.Entity
// import jakarta.persistence.EnumType
// import jakarta.persistence.Enumerated
// import jakarta.persistence.Id
// import jakarta.persistence.Table
// import site.weixing.natty.domain.dictionary.dictionary.DictionaryState.DictionaryStatus
//
// /**
// * 字典查询实体
// *
// * @param id 字典ID
// * @param code 字典编码
// * @param name 字典名称
// * @param description 字典描述
// * @param status 字典状态
// */
// @Entity
// @Table(name = "dictionary_read_model")
// data class DictionaryEntity(
//    @Id
//    val id: String,
//
//    @Column(unique = true, nullable = false)
//    val code: String,
//
//    @Column(nullable = false)
//    var name: String,
//
//    var description: String? = null,
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    var status: DictionaryStatus
// )
