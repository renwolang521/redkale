/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.service;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 本地模式注解。
 * 声明为LocalService的Service将不会变成远程模式，只能以本地模式存在， 无论配置文件中是否配置成远程模式都会被忽略。
 *
 * <p> 详情见: http://redkale.org
 * @author zhangjx
 */
@Inherited
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface LocalService {
    
}
