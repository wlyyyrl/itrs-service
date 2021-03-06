package org.wlyyy.itrs.service;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.wlyyy.itrs.spring.ItrsBoot;

import java.util.HashSet;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ItrsBoot.class)
@Transactional
public class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Test
    public void testUpdateUserRole() {
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(1l);
        roleIds.add(4l);
        roleIds.add(3l);
        roleService.updateUserRole(8l, roleIds);

        // Set<Long> userIds = roleService.findRoleIdsByUserId(8l).getData();
        // System.out.println(userIds);

        Set<Long> roleIdss = roleService.findUserIdsByRoleId(1l).getData();
        System.out.println(roleIdss);
    }
}
