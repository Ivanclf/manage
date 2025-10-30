drop database if exists school;
create database school;
use school;

create table administrator(
	id int unsigned primary key auto_increment comment '管理员id',
    user_name varchar(30) unique not null comment '管理员账号',
    user_password char(32) not null comment '管理员密码（经过MD5加密）',
    
    index idx_name (user_name) comment '账号索引'
) comment '管理员表';

create table activity(
	-- 核心信息
	id bigint unsigned primary key comment '活动id',
	activity_name varchar(50) not null comment '活动名称',
    activity_description varchar(1000) comment '活动简介',
    status tinyint default 1 not null comment '1. 未发布，2. 未开始报名，3. 报名中，4. 未开始，5. 进行中，6. 已结束',
    -- 位置信息
    latitude decimal(10, 8) not null comment '纬度',
    longitude decimal(11, 8) not null comment '经度',
    location varchar(200) not null comment '位置描述',
    -- URL字段
    qr_code_oss_url varchar(300) comment '二维码OSS存储URL',
    link varchar(500) comment '相关链接',    
    -- 时间
    registration_start datetime not null comment '报名开始时间',
    registration_end datetime not null comment '报名结束时间',
    activity_start datetime not null comment '活动开始时间',
    activity_end datetime not null comment '活动结束时间',
    -- 发布信息
    release_time datetime comment '发布时间',
    creator_id int unsigned comment '创建者',
    create_time datetime default now() comment '创建时间',
    update_time datetime on update now() comment '更新时间',
    -- 人数信息
    max_participants int unsigned not null default 0 comment '最大报名人数',
    current_participants int unsigned not null default 0 comment '目前报名人数',
    -- 索引
    index idx_registeration_time (registration_start, registration_end) comment '报名时间索引',
    index idx_activity_time (activity_start, activity_end) comment '活动时间索引',
    index idx_status (status) comment '状态索引',
    index idx_creator (creator_id) comment '创建者索引',
    foreign key (creator_id) references administrator(id)
) comment '活动表';

create table registration(
	-- 关联信息
	id bigint primary key auto_increment comment '报名记录id',
    activity_id bigint unsigned not null comment '活动id',
    -- 报名者信息
    registration_name varchar(30) not null comment '姓名',
    college varchar(20) comment '学院',
    phone char(11) not null comment '手机号',
    registration_time datetime default now() comment '报名时间',
    checkin tinyint default 0 comment '0. 未签到，1. 已签到',
    -- 索引
    unique index idx_acticity_phone (activity_id, phone) comment '报名信息索引',
    index idx_activity_id (activity_id) comment '活动索引',
    index idx_phone (phone) comment '预约者索引',
    foreign key (activity_id) references activity (id) on delete cascade
) comment '报名记录表';

create table spring_session(
    primary_id char(36) not null,
    session_id char(36) not null,
    creation_time bigint not null,
    last_access_time bigint not null,
    max_inactive_interval int not null,
    expiry_time bigint not null,
    principal_name varchar(100),
    constraint spring_session_pk primary key (primary_id),
    
    unique index spring_session_ix1 (session_id),
    index spring_session_ix2 (expiry_time),
    index spring_session_ix3 (principal_name)
);

delimiter ;;
-- 增加报名人数
create trigger increase_register
after insert on registration
for each row
begin
	update activity
    set current_participants = current_participants + 1,
		update_time = now()
	where id = new.activity_id;
end;;
-- 减少报名人数
create trigger decrease_register
after delete on registration
for each row
begin
	update activity
    set current_participants = current_participants - 1,
		update_time = now()
	where id = old.activity_id;
end;;
delimiter ;