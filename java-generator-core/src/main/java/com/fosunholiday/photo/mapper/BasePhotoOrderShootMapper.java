package com.fosunholiday.photo.mapper;

import java.util.List;
import com.fosunholiday.photo.model.PhotoOrderShoot;

public interface BasePhotoOrderShootMapper {

/**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table photo_order_shoot
     *
     * @mbg.generated
     */
int deleteByPrimaryKey(String id);

/**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table photo_order_shoot
     *
     * @mbg.generated
     */
int insertSelective(PhotoOrderShoot record);

/**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table photo_order_shoot
     *
     * @mbg.generated
     */
PhotoOrderShoot selectByPrimaryKey(String id);

/**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table photo_order_shoot
     *
     * @mbg.generated
     */
List<PhotoOrderShoot> selectAll();

/**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table photo_order_shoot
     *
     * @mbg.generated
     */
List<PhotoOrderShoot> selectNotDeleteAll();

/**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table photo_order_shoot
     *
     * @mbg.generated
     */
PhotoOrderShoot selectNotDeleteByPrimaryKey(String id);

/**
	 * miaoshuxinx 
	 * @param id
	 * @return
	 */
PhotoOrderShoot selectNotDeleteByPrimaryKey11(String id);


}