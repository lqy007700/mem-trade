package org.trade.user;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.trade.ApiError;
import org.trade.ApiException;
import org.trade.enums.UserType;
import org.trade.model.ui.PasswordAuthEntity;
import org.trade.model.ui.UserEntity;
import org.trade.model.ui.UserProfileEntity;
import org.trade.support.AbstractDbService;
import org.trade.util.HashUtil;
import org.trade.util.RandomUtil;


@Component
@Transactional
public class UserService extends AbstractDbService {

    public UserProfileEntity getUserProfile(Long userId) {
        return db.get(UserProfileEntity.class, userId);
    }

    @Nullable
    public UserProfileEntity fetchUserProfileByEmail(String email) {
        return db.from(UserProfileEntity.class).where("email = ?", email).first();
    }

    public UserProfileEntity getUserProfileByEmail(String email) {
        UserProfileEntity userProfile = fetchUserProfileByEmail(email);
        if (userProfile == null) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED);
        }
        return userProfile;
    }

    public UserProfileEntity signup(String email, String name, String passwd) {
        final long ts = System.currentTimeMillis();
        // insert user:
        var user = new UserEntity();
        user.type = UserType.TRADER;
        user.createdAt = ts;
        db.insert(user);
        // insert user profile:
        var up = new UserProfileEntity();
        up.userId = user.id;
        up.email = email;
        up.name = name;
        up.createdAt = up.updatedAt = ts;
        db.insert(up);
        // insert password auth:
        var pa = new PasswordAuthEntity();
        pa.userId = user.id;
        pa.random = RandomUtil.createRandomString(32);
        pa.passwd = HashUtil.hmacSha256(passwd, pa.random);
        db.insert(pa);
        return up;
    }

    public UserProfileEntity signin(String email, String passwd) {
        UserProfileEntity userProfile = getUserProfileByEmail(email);
        // find PasswordAuthEntity by user id:
        PasswordAuthEntity pa = db.fetch(PasswordAuthEntity.class, userProfile.userId);
        if (pa == null) {
            throw new ApiException(ApiError.USER_CANNOT_SIGNIN);
        }
        // check password hash:
        String hash = HashUtil.hmacSha256(passwd, pa.random);
        if (!hash.equals(pa.passwd)) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED);
        }
        return userProfile;
    }
}
