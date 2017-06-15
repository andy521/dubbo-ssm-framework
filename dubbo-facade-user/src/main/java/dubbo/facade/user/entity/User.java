package dubbo.facade.user.entity;

import java.util.Date;

import dubbo.common.entity.BaseEntity;

/**
 * 用户
 */
public class User extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	private long userId;
	
	private String userName;
	
	private long userPhone;
	
	private Date createTime;
	
	private int score;
	
	private Integer status; // 状态(100:可用，101:不可用 )

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}


	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public long getUserPhone() {
		return userPhone;
	}

	public void setUserPhone(long userPhone) {
		this.userPhone = userPhone;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

}
