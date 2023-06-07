package cn.zflzqy.mysqldatatoes;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description: base
 * @Author: jeecg-boot
 * @Date:   2023-05-06
 * @Version: V1.0
 */
@Document(indexName = "t_base")
public class Base implements Serializable {
    private static final long serialVersionUID = 1L;

	/**租户号*/
    @Field(type = FieldType.Long)
    private String tenantId;
	/**创建人*/
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String createBy;
	/**创建时间*/
    @Field(type = FieldType.Date, format = DateFormat.custom,pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
	/**更新人*/
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String updateBy;
	/**更新时间*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
	/**产业分类*/
    private String industryClassification;
	/**基地编号*/
    @Field(type = FieldType.Long)
    private String no;
    /**主键*/
    @Id
    private String id;
	/**基地名称*/
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;
	/**环境描述*/
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String environment;
	/**经度*/
    @Field(type = FieldType.Text)
    private BigDecimal latitude;
	/**纬度*/
    @Field(type = FieldType.Text)
    private BigDecimal longitude;
}
