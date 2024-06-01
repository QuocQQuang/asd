var crypto = require ('crypto') ;
var uuid = require('uuid') ;
var express = require ('express') ;
var mysql = require ('mysql2') ;
var bodyParser = require ('body-parser') ;
// var axios = require('axios');

//Connect to MySQL
var con = mysql. createConnection ({
host: 'localhost', // Replace your HOST IP
user: 'root',
password:'mysql',
database: 'app'
});


//PASSWORD ULTIL
var getRandomString = function (length) {
    return crypto.randomBytes (Math.ceil (length/2))
    .toString('hex') /* convert to hexa format */
    .slice(0, length); /* return required number of characters */
}
var sha512 = function (password, salt) {
    var hash = crypto.createHmac('sha512',salt); // Use SHA512
    hash.update (password) ;
    var value = hash.digest ('hex');
    return {
        salt:salt,
        passwordHash:value
    }
}
function saltHashPassword(userPassword) {
    var salt = getRandomString(16);
    var passwordData = sha512(userPassword,salt)
    return passwordData
}
var app=express () ;
app.use (bodyParser.json ()); // Accept JSON Params
app.use (bodyParser.urlencoded({extended: true})); // Accept URL Encoded params

function checkHashPassword (userPassword, salt){
    var passwordData = sha512(userPassword, salt) ;
    return passwordData;
}
app.post('/register/',(req,res,next)=>{
    var post_data = req.body

    var uid = uuid.v4 (); // Get UUID v4 like '110abacsasas-af0x-90333-casasjkajksk
    var plaint_password = post_data.password; // Get password from post params
    var hash_data = saltHashPassword(plaint_password) ;
    var password = hash_data.passwordHash; // Get hash value
    var salt = hash_data.salt; // Get salt

    var name = post_data.name;
    var email = post_data.email;

    con.query ('SELECT * FROM `users` where email =? ', [email], function (err, result, fields) {
        con.on ('error', function (err) {
        console.log ('[MySQL ERROR] ', err) ; 
    });
    if (result && result.length)
        res.json ('User already exists !!! ');
    else
    {
        con.query('INSERT INTO `users`( `unique_id`, `username`, `email`, `encrypted_password`, `salt`, `created_at`, `updated_at`) VALUES (?,?,?,?,?,NOW(),NOW())', [uid, name, email, password, salt], function(err, result, fields){
            con.on('error', function(err){
                console.log('[MySQL ERROR]', err);  
                res.json('Register error: ', err);
            });
            res.json('Register successful');
        });
        
    }
})
})
app.post('/login/', (req, res, next) => {
    var post_data = req.body;
    var user_password = post_data.password;
    var email = post_data.email;

    con.query('SELECT * FROM users WHERE email = ?', [email], function (err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR] ', err);
            res.status(500).json('Internal server error');
            return;
        }

        if (result && result.length) {
            var salt = result[0].salt;
            var encrypted_password = result[0].encrypted_password;
            var hashed_password = checkHashPassword(user_password, salt).passwordHash;

            if (encrypted_password === hashed_password) {
                // Trả về thông tin người dùng dưới dạng đối tượng JSON
                res.json(result[0]);
            } else {
                res.status(401).json('Wrong password');
            }
        } else {
            res.status(404).json( {message:'User not exists !!!'});
        }
    });
});

app.post('/change-password', (req, res, next) => {
    var post_data = req.body;
    var email = post_data.email;
    var oldPassword = post_data.oldPassword;
    var newPassword = post_data.newPassword;

    con.query('SELECT * FROM users WHERE email = ?', [email], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR] ', err);
            res.status(500).json({ message:'Internal server error'});
            return;
        }

        if (result && result.length) {
            var salt = result[0].salt;
            var encrypted_password = result[0].encrypted_password;
            var hashed_old_password = checkHashPassword(oldPassword, salt).passwordHash;

            if (encrypted_password === hashed_old_password) {
                // Mã hóa mật khẩu mới
                var newHashedPasswordData = saltHashPassword(newPassword);
                var newEncryptedPassword = newHashedPasswordData.passwordHash;
                var newSalt = newHashedPasswordData.salt;

                // Cập nhật mật khẩu mới vào cơ sở dữ liệu
                con.query('UPDATE users SET encrypted_password = ?, salt = ?, updated_at = NOW() WHERE email = ?', 
                [newEncryptedPassword, newSalt, email], function(err, result, fields) {
                    if (err) {
                        console.log('[MySQL ERROR]', err);
                        res.status(500).json({ message:'Internal server error'});
                    } else {
                        res.json({ message:'Password updated successfully'});
                    }
                });
            } else {
                res.status(401).json({ message:'Wrong old password'});
            }
        } else {
            res.status(404).json({ message:'User not exists'});
        }
    });
});



async function queryDatabase(sql, params) {
    return new Promise((resolve, reject) => {
        con.query(sql, params, (err, result, fields) => {
            if (err) {
                reject(err);
            } else {
                resolve(result);
            }
        });
    });
}




//hiển thị lớp đã tham gia 
app.get('/class/user/:userId', async (req, res, next) => {
    try {
        var userId = req.params.userId;
        var searchQuery = req.query.search; // Thêm tham số tìm kiếm

        // Truy vấn các lớp mà người dùng đã tham gia, với điều kiện tìm kiếm
        let studentClasses;
        if (searchQuery) {
            studentClasses = await queryDatabase('SELECT * FROM `student` WHERE student_id = ? AND class_name LIKE ?', [userId, `%${searchQuery}%`]);
        } else {
            studentClasses = await queryDatabase('SELECT * FROM `student` WHERE student_id = ?', [userId]);
        }

        if (studentClasses.length === 0) {
            return res.status(404).json('No classes found for this user');
        }

        // Thu thập các class IDs
        var classIds = studentClasses.map(row => row.class_id);

        // Truy vấn chi tiết các lớp dựa trên class IDs, với điều kiện tìm kiếm
        let classes;
        if (searchQuery) {
            classes = await queryDatabase('SELECT * FROM `class` WHERE id IN (?) AND class_name LIKE ?', [classIds, `%${searchQuery}%`]);
        } else {
            classes = await queryDatabase('SELECT * FROM `class` WHERE id IN (?)', [classIds]);
        }

        res.json(classes);
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json('Internal server error');
    }
});


// Hiển thị các lớp mà người dùng làm admin
app.get('/admin-classes/:adminId', async (req, res, next) => {
    try {
        var adminId = req.params.adminId;
        // Truy vấn các lớp mà người dùng là admin
        const adminClasses = await queryDatabase('SELECT * FROM `class` WHERE admin = ?', [adminId]);

        if (adminClasses.length === 0) {
            return res.status(404).json({ message: 'No classes found for this admin' });
        }

        res.json(adminClasses);
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});


//hiển thị bài trong 1 lớp 
app.get('/post/:classId', (req, res, next) => {
    var classId = req.params.classId;
    con.query('SELECT * FROM `posts` WHERE class_id = ?', [classId], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR]', err);
            res.status(500).json('Internal server error');
        } else {
            res.json(result);
        }
    });
});

//nộp bài
app.post('/submit', (req, res, next) => {
    var submissionData = req.body;
    var assignmentId = submissionData.assignment_id;
    var studentId = submissionData.student_id;
    var submissionDate = new Date();
    var submissionContent = submissionData.submission_content;

    con.query('INSERT INTO `submissions` (assignment_id, student_id, submission_date, submission_content) VALUES (?, ?, ?, ?)', [assignmentId, studentId, submissionDate, submissionContent], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR]', err);
            res.status(500).json('Internal server error');
        } else {
            res.json('Submission successful');
        }
    });
});

// Tạo bài tập (chỉ cho phép admin của lớp tạo bài tập)
app.post('/post', async (req, res, next) => {
    var postData = req.body;
    var classId = postData.class_id;
    var authorId = postData.author_id;
    var dayEnd = postData.day_end;
    var postName = postData.post_name;
    var postContent = postData.post_content;
    var linkDrive = postData.link_drive; // Thêm link_drive từ body request

    try {
        // Kiểm tra xem người dùng có phải là admin của lớp không
        const classResult = await queryDatabase('SELECT admin FROM class WHERE id = ?', [classId]);

        if (classResult.length === 0) {
            return res.status(404).json({ message: 'Class not found' });
        }

        var adminId = classResult[0].admin;

        if (adminId != authorId) {
            return res.status(403).json({ message: 'You are not authorized to create a post for this class' });
        }

        console.log('Attempting to create a post with classId:', classId, 'authorId:', authorId);
        console.log('Admin ID for classId', classId, 'is', adminId);

        // Nếu người dùng là admin, tiếp tục tạo bài
        con.query('INSERT INTO `posts` (class_id, author_id, post_name, post_content, day_created, day_end, link_drive) VALUES (?, ?, ?, ?, NOW(), ?, ?)', 
                  [classId, authorId, postName, postContent, dayEnd, linkDrive], 
                  function (err, result, fields) {
            if (err) {
                console.log('[MySQL ERROR]', err);
                res.status(500).json({ message:'Internal server error'});
            } else {
                res.json({ message:'Post created'});
            }
        });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message:'Internal server error'});
    }
});

// Tạo bài tập (chỉ cho phép admin của lớp tạo bài tập)
app.post('/post', async (req, res, next) => {
    var postData = req.body;
    var classId = postData.class_id;
    var authorId = postData.author_id;
    var dayEnd = postData.day_end;
    var postName = postData.post_name;
    var postContent = postData.post_content;
    var linkDrive = postData.link_drive || ''; 

    try {
        // Kiểm tra xem người dùng có phải là admin của lớp không
        const classResult = await queryDatabase('SELECT admin FROM class WHERE id = ?', [classId]);

        if (classResult.length === 0) {
            return res.status(404).json({ message: 'Class not found' });
        }

        var adminId = classResult[0].admin;

        if (adminId != authorId) {
            return res.status(403).json({ message: 'You are not authorized to create a post for this class' });
        }

        console.log('Attempting to create a post with classId:', classId, 'authorId:', authorId);
        console.log('Admin ID for classId', classId, 'is', adminId);

        // Nếu người dùng là admin, tiếp tục tạo bài
        con.query('INSERT INTO `posts` (class_id, author_id, post_name, post_content, day_created, day_end, link_drive) VALUES (?, ?, ?, ?, NOW(), ?, ?)', 
                  [classId, authorId, postName, postContent, dayEnd, linkDrive], 
                  function (err, result, fields) {
            if (err) {
                console.log('[MySQL ERROR]', err);
                res.status(500).json({ message:'Internal server error'});
            } else {
                res.json({ message:'Post created'});
            }
        });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message:'Internal server error'});
    }
});





//tạo lớp mới
app.post('/create-class/', (req, res, next) => {
    const { title, adminId } = req.body;
    var author_name = req.body.admin_name;

    console.log('Creating class with title:', title, 'and adminId:', adminId);

    con.query('INSERT INTO `class`(`title`, `admin`,`admin_name`) VALUES (?, ?,?)', [title, adminId,author_name], (err, result) => {
        if (err) {
            console.error('[MySQL ERROR]', err);
            return res.status(500).json({ error: 'Create class error', details: err });
        }

        console.log('Class created successfully:', result);
        res.json({ message: 'Class created successfully', result });
    });
});

// Tham gia lớp bằng ID lớp
app.post('/join-class/:classId', (req, res, next) => {
    var classId = req.params.classId;
    var studentId = req.body.studentId; // Nhận ID của học sinh từ body request

    // Kiểm tra xem học sinh đã tham gia lớp chưa
    con.query('SELECT * FROM `student` WHERE student_id = ? AND class_id = ?', [studentId, classId], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR]', err);
            res.status(500).json('Internal server error');
        } else {
            // Nếu học sinh đã tham gia lớp, không thực hiện thêm một lần nữa
            if (result && result.length > 0) {
                res.json('Đã tham gia lớp này!!!');
            } else {
                // Thêm học sinh vào lớp
                con.query('INSERT INTO `student` (class_id, student_id) VALUES (?, ?)', [classId, studentId], function(err, result, fields) {
                    if (err) {
                        console.log('[MySQL ERROR]', err);
                        res.status(500).json('Internal server error');
                    } else {
                        res.json('Tham gia thành công!');
                    }
                });
            }
        }
    });
});
// Thoát lớp bằng ID lớp
app.post('/leave-class/:classId', (req, res, next) => {
    var classId = req.params.classId;
    var studentId = req.body.studentId; // Nhận ID của học sinh từ body request

    // Kiểm tra xem học sinh có tham gia lớp không
    con.query('SELECT * FROM `student` WHERE student_id = ? AND class_id = ?', [studentId, classId], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR]', err);
            res.status(500).json('Internal server error');
        } else {
            // Nếu học sinh chưa tham gia lớp, không thực hiện gì thêm
            if (result && result.length === 0) {
                res.json('Chưa tham gia lớp này!!!');
            } else {
                // Xóa học sinh khỏi lớp
                con.query('DELETE FROM `student` WHERE student_id = ? AND class_id = ?', [studentId, classId], function(err, result, fields) {
                    if (err) {
                        console.log('[MySQL ERROR]', err);
                        res.status(500).json('Internal server error');
                    } else {
                        res.json('Rời khỏi lớp thành công!');
                    }
                });
            }
        }
    });
});
app.put('/edit-post/:postId', async (req, res, next) => {
    try {
        var postId = req.params.postId;
        var userId = req.body.userId; 

        // Kiểm tra xem người gửi yêu cầu có phải là admin của lớp không
        const classResult = await queryDatabase('SELECT author_id FROM `posts` WHERE post_id = ?', [postId]);

        var adminId = classResult[0].author_id;

        if (adminId != userId) {
            return res.status(403).json({ message: 'You are not authorized to edit this post' });
        }

        // Nếu người gửi yêu cầu là admin, tiếp tục chỉnh sửa bài viết
        var newPostName = req.body.post_name;
        var newPostContent = req.body.post_content;
        var newLink_Drive = req.body.link_drive
        var newDay_End = req.body.day_end
        con.query('UPDATE `posts` SET post_name = ?, post_content = ?, link_drive = ?, day_end = ? WHERE post_id = ?', [newPostName, newPostContent, newLink_Drive, newDay_End, postId], function (err, result, fields) {
            if (err) {
                console.log('[MySQL ERROR]', err);
                res.status(500).json({ message: 'Internal server error' });
            } else {
                res.json({ message: 'Post edited' });
            }
        });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});


// Tìm bài đăng bằng post_id
app.get('/post-by-id/:postId', (req, res, next) => {
    var postId = req.params.postId;
    
    con.query('SELECT * FROM `posts` WHERE post_id = ?', [postId], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR]', err);
            res.status(500).json('Internal server error');
        } else if (result.length === 0) {
            res.status(404).json('Post not found');
        } else {
            res.json(result[0]);
        }
    });
});
// Xóa bài tập (chỉ cho phép admin của lớp xóa bài tập)
app.post('/delete-post/:postId', async (req, res, next) => {
    try {
        var postId = req.params.postId;
        var authorId = req.body.author_id; 

        // Kiểm tra xem người dùng có phải là admin của lớp chứa bài tập không
        const classResult = await queryDatabase('SELECT author_id FROM `posts` WHERE post_id = ?', [postId]);

        var adminId = classResult[0].author_id;

        if (adminId != authorId) {
            return res.status(403).json({ message: 'You are not authorized to delete this post' });
        }

        // Nếu người dùng là admin, tiếp tục xóa bài tập
        con.query('DELETE FROM `posts` WHERE post_id = ?', [postId], function (err, result, fields) {
            if (err) {
                console.log('[MySQL ERROR]', err);
                res.status(500).json({ message: 'Internal server error' });
            } else {
                res.json({ message: 'Post deleted' });
            }
        });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});
app.post('/remove-student/:classId', async (req, res, next) => {
    try {
        var classId = req.params.classId;
        var adminId = req.body.adminId;
        var studentId = req.body.studentId;

        // Kiểm tra xem người gửi yêu cầu có phải là admin của lớp không
        const classResult = await queryDatabase('SELECT admin FROM `class` WHERE id = ?', [classId]);

        if (classResult.length === 0) {
            return res.status(404).json({ message: 'Không tìm thấy lớp học' });
        }

        var classAdminId = classResult[0].admin;

        if (classAdminId != adminId) {
            return res.status(403).json({ message: 'Bạn không có quyền xóa học sinh khỏi lớp này' });
        }

        // Xóa học sinh khỏi lớp
        const deleteResult = await queryDatabase('DELETE FROM student WHERE student_id = ? AND class_id = ?', [studentId, classId]);

        if (deleteResult.affectedRows === 0) {
            return res.status(404).json({ message: 'Không tìm thấy học sinh trong lớp này' });
        }

        res.json({ message: 'Xóa học sinh khỏi lớp thành công' });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Lỗi máy chủ nội bộ' });
    }
});

app.get('/check-assignments/:classId', async (req, res, next) => {
    try {
        var classId = req.params.classId;

        // Get the current date
        var currentDate = new Date();
        
        // Define the threshold for approaching due date (e.g., 7 days)
        var threshold = 7;
        var thresholdDate = new Date(currentDate);
        thresholdDate.setDate(thresholdDate.getDate() + threshold);

        // Query assignments for the specified class
        const assignments = await queryDatabase('SELECT * FROM `posts` WHERE class_id = ?', [classId]);

        if (assignments.length === 0) {
            return res.status(404).json({ message: 'Không có bài tập nào cho lớp này' });
        }

        // Check each assignment's due date
        var overdueAssignments = assignments.filter(assignment => {
            var dueDate = new Date(assignment.day_end);
            return dueDate < currentDate;
        });

        var approachingAssignments = assignments.filter(assignment => {
            var dueDate = new Date(assignment.day_end);
            return dueDate >= currentDate && dueDate <= thresholdDate;
        });

        var response = {
            message: 'Bài tập đến hạn:',
            approachingAssignments: approachingAssignments.map(assignment => `Bài tập "${assignment.post_name}" gần đến hạn!`)
        };

        res.json(response);
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});
// Kiểm tra trạng thái nộp bài của học sinh
app.get('/submission-status/:assignmentId/:studentId', (req, res, next) => {
    var assignmentId = req.params.assignmentId;
    var studentId = req.params.studentId;

    con.query('SELECT * FROM `submissions` WHERE assignment_id = ? AND student_id = ?', [assignmentId, studentId], function(err, result, fields) {
        if (err) {
            console.log('[MySQL ERROR]', err);
            res.status(500).json('Internal server error');
        } else {
            if (result && result.length > 0) {
                res.json({ submitted: true });
            } else {
                res.json({ submitted: false });
            }
        }
    });
});
// Kiểm tra bài tập đã hết hạn chưa
app.get('/check-expired/:assignmentId', async (req, res, next) => {
    var assignmentId = req.params.assignmentId;

    try {
        // Lấy ngày và giờ hiện tại
        const currentDate = new Date();

        // Truy vấn cơ sở dữ liệu để lấy thông tin về bài tập với assignmentId nhất định
        const assignmentInfo = await queryDatabase('SELECT day_end FROM posts WHERE post_id = ?', [assignmentId]);

        if (assignmentInfo.length === 0) {
            // Trả về một thông báo rằng không tìm thấy thông tin về bài tập
            res.status(404).json({ message: 'Không tìm thấy thông tin về bài tập.' });
            return;
        }

        // Lấy ngày kết thúc của bài tập
        const endDate = new Date(assignmentInfo[0].day_end);

        // So sánh ngày và giờ kết thúc với ngày và giờ hiện tại
        if (currentDate > endDate) {
            // Trả về true nếu bài tập đã hết hạn
            res.json({ expired: true });
        } else {
            // Trả về false nếu bài tập vẫn chưa hết hạn
            res.json({ expired: false });
        }
    } catch (error) {
        console.error('Lỗi khi kiểm tra bài tập đã hết hạn:', error);
        res.status(500).json({ message: 'Đã xảy ra lỗi khi kiểm tra bài tập đã hết hạn.' });
    }
});

// hiển thị list học sinh trong 1 lớp 
app.get('/class-users/:classId', async (req, res, next) => {
    try {
        var classId = req.params.classId;

        // Truy vấn tất cả người dùng trong lớp
        const users = await queryDatabase('SELECT u.* FROM `users` u INNER JOIN `student` s ON u.id = s.student_id WHERE s.class_id = ?', [classId]);

        if (users.length === 0) {
            return res.status(404).json({ message: 'Không có học sính nào trong lớp này' });
        }

        res.json(users);
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Lỗi máy chủ nội bộ' });
    }
});
///////////////////////
app.get('/submitted-users/:postId', async (req, res, next) => {
    try {
        var postId = req.params.postId;

        const submittedUsers = await queryDatabase('SELECT users.* FROM users INNER JOIN submissions ON users.id = submissions.student_id WHERE submissions.assignment_id = ?', [postId]);

        res.json({ users: submittedUsers });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});

app.get('/not-submitted-users/:postId', async (req, res, next) => {
    try {
        var postId = req.params.postId;

        const allUsers = await queryDatabase('SELECT * FROM users');
        const submittedUsers = await queryDatabase('SELECT users.* FROM users INNER JOIN submissions ON users.id = submissions.student_id WHERE submissions.assignment_id = ?', [postId]);

        const notSubmittedUsers = allUsers.filter(user => !submittedUsers.some(submittedUser => submittedUser.id === user.id));

        res.json({ users: notSubmittedUsers });
    } catch (err) {
        console.log('[MySQL ERROR]', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});

//Start Server
app.listen(3000, ()=>{
    console.log ('Restful running on port 3000');
});



